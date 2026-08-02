# ADR-022: Postgres runs in Docker on a small, dedicated VM rather than a managed database service --
# see the ADR for the cost rationale and the operational risks this trades in for it.

resource "azurerm_public_ip" "postgres_vm" {
  name                = "pip-postgres-vm-${var.environment}"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  allocation_method   = "Static"
  sku                 = "Standard"
  tags                = var.tags
}

resource "azurerm_network_interface" "postgres_vm" {
  name                = "nic-postgres-vm-${var.environment}"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  tags                = var.tags

  ip_configuration {
    name                          = "internal"
    subnet_id                     = azurerm_subnet.vm.id
    private_ip_address_allocation = "Static"
    private_ip_address            = "10.20.1.4"
    public_ip_address_id          = azurerm_public_ip.postgres_vm.id
  }
}

resource "azurerm_linux_virtual_machine" "postgres" {
  name                = "vm-postgres-${var.environment}"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  size                = "Standard_B1s" # 1 vCPU / 1GiB RAM -- see ADR-022 for the RAM-tuning tradeoff this implies
  admin_username      = var.vm_admin_username
  network_interface_ids = [
    azurerm_network_interface.postgres_vm.id,
  ]
  tags = var.tags

  # Key-based SSH only -- password authentication is never enabled.
  disable_password_authentication = true
  admin_ssh_key {
    username   = var.vm_admin_username
    public_key = var.vm_ssh_public_key
  }

  os_disk {
    name                 = "osdisk-postgres-${var.environment}"
    caching              = "ReadWrite"
    storage_account_type = "StandardSSD_LRS"
    disk_size_gb         = 32 # Postgres data lives on this disk too (via a Docker volume) -- see cloud-init. A separate data disk is a natural upgrade later for independent snapshotting, but would double the disk line item in the cost estimate, so kept as one disk for now.
  }

  source_image_reference {
    publisher = "Canonical"
    offer     = "0001-com-ubuntu-server-jammy"
    sku       = "22_04-lts-gen2"
    version   = "latest"
  }

  # System-assigned managed identity -- used by the backup script (infra/scripts/backup-postgres.sh)
  # to authenticate to Blob Storage via `az login --identity` instead of embedding a storage account
  # key in cloud-init's custom_data (which is stored on the VM and readable by anything with root,
  # even if not directly exposed outside it -- a managed identity avoids that class of secret
  # entirely).
  identity {
    type = "SystemAssigned"
  }

  custom_data = base64encode(templatefile("${path.module}/../cloud-init/postgres-vm.yaml", {
    postgres_db_name        = var.postgres_db_name
    postgres_admin_user     = var.postgres_admin_user
    postgres_admin_password = var.postgres_admin_password
    postgres_private_ip     = azurerm_network_interface.postgres_vm.private_ip_address
    backup_storage_account  = azurerm_storage_account.backups.name
    backup_container_name   = azurerm_storage_container.postgres_backups.name
    # Read once, statically, so infra/scripts/backup-postgres.sh remains a plain, independently
    # shellcheck-able script -- Terraform embeds its raw bytes here rather than templating it.
    backup_script_b64 = base64encode(file("${path.module}/../scripts/backup-postgres.sh"))
  }))
}

# Grants the VM's managed identity permission to write backup blobs -- scoped to exactly this one
# storage account, nothing broader.
resource "azurerm_role_assignment" "postgres_vm_backup_writer" {
  scope                = azurerm_storage_account.backups.id
  role_definition_name = "Storage Blob Data Contributor"
  principal_id         = azurerm_linux_virtual_machine.postgres.identity[0].principal_id
}
