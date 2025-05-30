const { merge } = require('webpack-merge');
const common = require('./webpack.common');

const webpack = require('webpack');

// In your plugins array:
plugins: [
  // ...other plugins,
  new webpack.DefinePlugin({
    'process.env.REACT_APP_BACKEND_URI': JSON.stringify(process.env.REACT_APP_BACKEND_URI),
  }),
],

module.exports = merge(common, {
  mode: 'production',
});
