const path = require('path');

module.exports = {
  entry: {
    index: ['./frontend/javascript/index.jsx'],
  },
  output: {
    path: path.resolve(__dirname, 'frontend/static'),
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
    ],
  },
  resolve: {
    extensions: ['.js', '.jsx'],
  },
  devServer: {
    port: 8022,
    allowedHosts: ['localhost'],
    proxy: {
      '/api': {
        target: 'http://localhost:8022',
        router: () => 'http://localhost:7032',
      },
    },
    static: {
      directory: './frontend/static',
    },
  },
};
