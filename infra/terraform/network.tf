# ADR-022: one VNet shared by the Postgres VM and the (VNet-integrated) Container Apps environment,
# so the API can reach Postgres over a private address with zero public exposure of port 5432.

resource "azurerm_virtual_network" "main" {
  name                = "vnet-${var.project}-${var.environment}"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  address_space       = ["10.20.0.0/16"]
  tags                = var.tags
}

# --- Postgres VM subnet -----------------------------------------------------------------------------

resource "azurerm_subnet" "vm" {
  name                 = "snet-vm"
  resource_group_name  = azurerm_resource_group.main.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = ["10.20.1.0/24"]
}

resource "azurerm_network_security_group" "vm" {
  name                = "nsg-vm-${var.environment}"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  tags                = var.tags

  security_rule {
    name                       = "AllowSSHFromAdmin"
    priority                   = 100
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "22"
    source_address_prefix      = var.admin_source_cidr
    destination_address_prefix = "*"
  }

  security_rule {
    name                       = "AllowPostgresFromVNetOnly"
    priority                   = 110
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "5432"
    source_address_prefix      = "VirtualNetwork"
    destination_address_prefix = "*"
  }

  # Everything else inbound is implicitly denied by Azure's default DenyAllInbound rule (priority
  # 65500) -- no explicit deny rule needed here, but noted so this isn't mistaken for an oversight.

  security_rule {
    name                       = "AllowInternetOutbound"
    priority                   = 100
    direction                  = "Outbound"
    access                     = "Allow"
    protocol                   = "*"
    source_port_range          = "*"
    destination_port_range     = "*"
    source_address_prefix      = "*"
    destination_address_prefix = "Internet"
  }
}

resource "azurerm_subnet_network_security_group_association" "vm" {
  subnet_id                 = azurerm_subnet.vm.id
  network_security_group_id = azurerm_network_security_group.vm.id
}

# --- Container Apps environment subnet ----------------------------------------------------------

# Delegated subnet for the VNet-integrated Container Apps environment. Sized at /23 (512 addresses):
# this was the documented minimum for a Consumption-only Container Apps environment at the time this
# was written, but Microsoft has changed this requirement across provider/platform versions before --
# verify against https://learn.microsoft.com/azure/container-apps/vnet-custom before the first apply,
# and resize here if it's changed (a /23 is deliberately generous headroom either way).
resource "azurerm_subnet" "container_apps" {
  name                 = "snet-container-apps"
  resource_group_name  = azurerm_resource_group.main.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = ["10.20.2.0/23"]

  delegation {
    name = "container-apps-delegation"

    service_delegation {
      name    = "Microsoft.App/environments"
      actions = ["Microsoft.Network/virtualNetworks/subnets/join/action"]
    }
  }
}

# No NSG is attached to this subnet: the Container Apps platform requires broad outbound access
# (pulling images, talking to the Azure control plane, DNS, etc.) and a misconfigured NSG here is a
# well-documented way to break the environment outright. The VM subnet's NSG is what actually matters
# for this deployment's security posture (SSH + Postgres access control) -- Postgres itself is never
# reachable from outside the VNet regardless of this subnet's configuration.
