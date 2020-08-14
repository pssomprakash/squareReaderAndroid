var exec = require('cordova/exec');

// Needs to match <feature name="..."> in plugin.xml
var FEATURE_NAME = 'SquareReaderAndroid';

exports.authorizeCode = function(code, success, error) {
  exec(success, error, FEATURE_NAME, 'authorizeCode', [code]);
};

exports.deauthorize = function(success, error) {
  exec(success, error, FEATURE_NAME, 'deauthorize', []);
}

exports.isAuthorized = function(success, error) {
  exec(success, error, FEATURE_NAME, 'isAuthorized', []);
}

exports.canDeauthorize = function(success, error) {
  exec(success, error, FEATURE_NAME, 'canDeauthorize', []);
}

exports.startCheckout = function(amountInCents, currencyCode, success, error) {
  exec(success, error, FEATURE_NAME, 'startCheckout', [amountInCents, currencyCode]);
};

exports.openReaderSettings = function(success, error) {
  exec(success, error, FEATURE_NAME, 'openReaderSettings', []);
}