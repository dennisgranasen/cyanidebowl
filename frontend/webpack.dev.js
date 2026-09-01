const { merge } = require('webpack-merge');
const common = require('./webpack.common');

module.exports = merge(common, {
  mode: 'development',
  devtool: 'inline-source-map',
  watchOptions: {
    ignored: ['**/node_modules/**', '**/public/**'],
  },
  devServer: {
    port: 8022,
    allowedHosts: ['localhost'],
    static: {
      directory: './public',
      watch: false,
    },
  },
});
