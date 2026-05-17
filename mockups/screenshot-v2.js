const { chromium } = require('/tmp/node_modules/playwright');
const path = require('path');

(async () => {
  const browser = await chromium.launch();
  const files = [
    'v2-calendar-light.html',
    'v2-calendar-dark.html',
    'v2-log-sheet.html',
    'v2-stats.html',
    'v2-lock.html',
    'v2-onboarding-goal.html',
  ];
  for (const file of files) {
    const page = await browser.newPage({ viewport: { width: 480, height: 900 }, deviceScaleFactor: 2 });
    await page.goto('file:///home/hayate0726/cycles/mockups/' + file);
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(500);
    const out = '/home/hayate0726/cycles/mockups/' + file.replace('.html', '.png');
    await page.screenshot({ path: out });
    console.log('Saved', out);
    await page.close();
  }
  await browser.close();
})();
