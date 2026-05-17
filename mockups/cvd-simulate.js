// CVD simulation using daltonization-style matrix transforms
// Source: Brettel/Vienot/Mollon (1997), commonly used matrices

const { chromium } = require('/tmp/node_modules/playwright');
const path = require('path');
const fs = require('fs');

// SVG color matrices for each CVD type
const filters = {
  deuteranopia: '0.625 0.375 0 0 0  0.7 0.3 0 0 0  0 0.3 0.7 0 0  0 0 0 1 0',
  protanopia:   '0.567 0.433 0 0 0  0.558 0.442 0 0 0  0 0.242 0.758 0 0  0 0 0 1 0',
  tritanopia:   '0.95 0.05 0 0 0  0 0.433 0.567 0 0  0 0.475 0.525 0 0  0 0 0 1 0',
};

(async () => {
  const browser = await chromium.launch();
  const files = [
    'direction-1-monochrome.html',
    'direction-2-materialyou.html',
    'direction-3-warmprivacy.html',
  ];

  for (const file of files) {
    for (const [cvd, matrix] of Object.entries(filters)) {
      // wrap the page with a CVD filter
      const wrapperHtml = `<!DOCTYPE html><html><head><style>
        body { margin: 0; padding: 0; }
        svg { position: fixed; width: 0; height: 0; }
        iframe { width: 100vw; height: 100vh; border: 0; filter: url(#cvd); }
      </style></head><body>
        <svg xmlns="http://www.w3.org/2000/svg">
          <defs><filter id="cvd"><feColorMatrix type="matrix" values="${matrix}"/></filter></defs>
        </svg>
        <iframe src="${file}"></iframe>
      </body></html>`;

      const wrapPath = '/home/hayate0726/cycles/mockups/__wrap_' + cvd + '_' + file;
      fs.writeFileSync(wrapPath, wrapperHtml);

      const page = await browser.newPage({ viewport: { width: 480, height: 880 }, deviceScaleFactor: 2 });
      await page.goto('file://' + wrapPath);
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(800);
      const out = '/home/hayate0726/cycles/mockups/' + file.replace('.html', '__' + cvd + '.png');
      await page.screenshot({ path: out });
      console.log('Saved', out);
      await page.close();
      fs.unlinkSync(wrapPath);
    }
  }

  await browser.close();
})();
