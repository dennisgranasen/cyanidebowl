const path = require('path');

module.exports = {
    entry: {
        index: ['./src/main/resources/javascript/index.jsx'],
    },
    output: {
        path: path.resolve(__dirname, 'src/main/resources/static'),
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
            }
        ],
    },
    resolve: {
        extensions: ['.js', '.jsx'],
    },
    devServer: {
        port: 8022,
        allowedHosts: [
            'localhost',
            'home.wawuschels.de'
        ],
        proxy: {
            'http://home.wawuschels.de:8080/api': {
                target: 'http://home.wawuschels.de:8080',
                router: () => 'http://localhost:7032',
            },
            '/api': {
                target: 'http://localhost:8022',
                router: () => 'http://localhost:7032',
            },
        },
        static: {
            directory: './src/main/resources/static',
        },
    },
};
