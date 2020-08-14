#!/usr/bin/env node

module.exports = function(context) {

  console.log('INSIDE HOOK!');

  var APPLICATION_CLASS = "com.om.squareReaderAndroid.SquareReaderApplication";
	console.log('TCL: APPLICATION_CLASS', APPLICATION_CLASS);

  // var fs = context.requireCordovaModule('fs'),
  // path = context.requireCordovaModule('path');
  var fs = require('fs');
  path = require('path');

  var platformRoot = path.join(context.opts.projectRoot, 'platforms/android');
  var pathToManifestFromRoot = 'app/src/main/AndroidManifest.xml';
  var manifestFile = path.join(platformRoot, pathToManifestFromRoot);
	// console.log('manifestFile path', manifestFile);


  if (fs.existsSync(manifestFile)) {
		console.log('Found manifest file!');
    fs.readFile(manifestFile, 'utf8', function (err, data) {
      if (err) {
        console.log('Unable to read AndroidManifest.xml:', err);
        throw new Error('Unable to read AndroidManifest.xml: ' + err);
      }

      // console.log("daata", data);
      if (data.indexOf(APPLICATION_CLASS) == -1) {
        // TODO: should replace existing android:name if present
        console.log("APPLICATION_CLASS", APPLICATION_CLASS);
        var result = data.replace(/<application/g, '<application android:name="' + APPLICATION_CLASS + '"');
        fs.writeFile(manifestFile, result, 'utf8', function (err) {
          if (err) {
            console.log('Unable to write into AndroidManifest.xml:', err);
            throw new Error('Unable to write into AndroidManifest.xml: ' + err);
          }
        })
      }
    });
  } else {
    console.log('Could not find manifest file!');
  }
};