const path = require('path');

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
  resolve: {
    extensions: ['.js', '.jsx'],
  },
};
