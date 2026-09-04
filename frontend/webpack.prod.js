const webpack = require('webpack');
const { merge } = require('webpack-merge');
const common = require('./webpack.common');

module.exports = merge(common, {
  mode: 'production',
  plugins: [
    new webpack.EnvironmentPlugin({
      REACT_APP_BACKEND_URI: '',
      REACT_APP_AUTH0_DOMAIN: '',
      REACT_APP_AUTH0_CLIENT_ID: '',
      REACT_APP_AUTH0_AUDIENCE: '',
    }),
  ],
});