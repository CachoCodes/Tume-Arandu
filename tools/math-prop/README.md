# sin(30°) map prop

This small Three.js scene is the source for `app/src/main/res/drawable-nodpi/sin30_sculpture.png`. It uses the installed Google Chrome through `playwright-core`; npm installs these two local build-only packages and does not install a browser.

From this directory, render or regenerate the asset with:

```sh
npm ci
npm run render
```

The render command writes a transparent 768×768 PNG directly to the Android drawable resource. To choose another output path, use `npm run render -- --output=/absolute/path/file.png`.
