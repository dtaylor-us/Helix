import playwright from '../../apps/web/node_modules/playwright/index.js';
const { chromium } = playwright;
const browser = await chromium.launch({ executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome', headless: true });
const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
for (const route of ['/transformations','/library','/search','/knowledge','/settings/memory','/settings']) {
  await page.goto('http://localhost:5173' + route, { waitUntil: 'networkidle' });
  const data = await page.locator('main').evaluate(main => ({
    text: main.innerText,
    controls: [...main.querySelectorAll('a,button,input,textarea,select')].map(e => ({tag:e.tagName,text:e.innerText||'',name:e.getAttribute('name')||'',type:e.getAttribute('type')||'',placeholder:e.getAttribute('placeholder')||'',aria:e.getAttribute('aria-label')||''}))
  }));
  console.log('\nROUTE', route, JSON.stringify(data, null, 2));
}
await browser.close();
