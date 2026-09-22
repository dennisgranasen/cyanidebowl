const path = require('path');
const webpack = require('webpack');

module.exports = {
  entry: {
    index: ['./src/index.jsx'],
  },
  output: {
    path: path.resolve(__dirname, './public'),
    filename: '[name].js',
  },
  module: {
    rules: [
      {
        test: [/\.js$/, /\.jsx$/],
        exclude: /node_modules/,
        use: {
          loader: 'babel-loader',
        },
      },
      {
        test: [/\.md$/],
        exclude: /node_modules/,
        use: {
          loader: 'raw-loader',
        },
      },
    ],
  },
  plugins: [
    new webpack.EnvironmentPlugin({
      REACT_APP_BACKEND_URI: '',
      REACT_APP_AUTH0_DOMAIN: '',
      REACT_APP_AUTH0_CLIENT_ID: '',
      REACT_APP_AUTH0_AUDIENCE: '',
    }),
  ],
  resolve: {
    extensions: ['.js', '.jsx'],
  },
};
