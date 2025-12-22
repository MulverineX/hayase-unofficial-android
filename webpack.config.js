import { join, resolve } from 'node:path'

import CopyWebpackPlugin from 'copy-webpack-plugin'
import webpack from 'webpack'
import LicensePlugin from 'webpack-license-plugin'
import 'webpack-dev-server'

const dirname = import.meta.dirname || new URL('.', import.meta.url).pathname

/** @type {import('webpack').Configuration[]} */
const config = [
  {
    entry: [join(dirname, 'src', 'background.ts')],
    output: {
      path: join(dirname, 'build', 'nodejs'),
      filename: 'index.js',
      publicPath: './'
    },
    module: {
      rules: [
        {
          test: /\.tsx?$/,
          use: [
            {
              loader: 'ts-loader',
              options: {
                transpileOnly: true
              }
            }
          ]
        }
      ]
    },
    externals: {
      'utp-native': 'require("utp-native")',
      bridge: 'require("bridge")',
      '@thaunknown/yencode': 'require("@thaunknown/yencode")',
      '@thaunknown/yencode/build/Release/yencode.node': 'require("@thaunknown/yencode/build/Release/yencode.node")'
    },
    resolve: {
      aliasFields: [],
      extensions: ['.ts', '.tsx', '.js', '.json'],
      mainFields: ['module', 'main', 'node'],
      alias: {
        wrtc: false,
        'node-datachannel': false,
        'http-tracker': resolve('./node_modules/bittorrent-tracker/lib/client/http-tracker.js'),
        'webrtc-polyfill': false // no webrtc on mobile, need the resources
      }
    },
    target: 'node',
    devServer: {
      devMiddleware: {
        writeToDisk: true
      },
      hot: false,
      client: false
    },
    plugins: [
      new CopyWebpackPlugin({ patterns: [{ from: join(dirname, 'public', 'nodejs') }] }),
      new webpack.optimize.LimitChunkCountPlugin({
        maxChunks: 1
      }),
      new LicensePlugin({
        outputFilename: 'index.js.LICENSE.txt',
        excludedPackageTest: (packageName) => packageName === 'torrent-client',
        licenseOverrides: {
          'compact2string@1.4.1': 'BSD-3-Clause'
        },
        additionalFiles: {
          'index.js.LICENSE.txt': (packages) => packages.map(({ name, version, license, licenseText, noticeText }) => `${name} ${version} (${license}) \n${noticeText ?? ''}\n${licenseText}`).join('\n\n')
        },
        unacceptableLicenseTest: (licenseId) => !['Apache-2.0', 'MIT', 'ISC', 'BSD-3-Clause', 'BSD-2-Clause'].includes(licenseId)
      })
    ]
  },
  {
    resolve: {
      extensions: ['.ts', '.tsx', '.js', '.json']
    },
    plugins: [
      new webpack.optimize.LimitChunkCountPlugin({
        maxChunks: 1
      }),
      new LicensePlugin({
        outputFilename: 'preload.js.LICENSE.txt',
        licenseOverrides: {
          'compact2string@1.4.1': 'BSD-3-Clause'
        },
        additionalFiles: {
          'preload.js.LICENSE.txt': (packages) => packages.map(({ name, version, license, licenseText, noticeText }) => `${name} ${version} (${license}) \n${noticeText ?? ''}\n${licenseText}`).join('\n\n')
        },
        unacceptableLicenseTest: (licenseId) => !['Apache-2.0', 'MIT', 'ISC', 'BSD-3-Clause', 'BSD-2-Clause'].includes(licenseId)
      })
    ],
    module: {
      rules: [
        {
          test: /\.m?js$/,
          resolve: {
            fullySpecified: false
          }
        },
        {
          test: /\.tsx?$/,
          use: [
            { loader: 'ts-loader', options: { transpileOnly: true } }
          ]
        }
      ]
    },
    entry: [join(dirname, 'src', 'preload.ts')],
    output: {
      path: join(dirname, 'build'),
      filename: 'preload.js',
      publicPath: './'
    }
  }
]

export default config
