#import "ScannerPlugin.h"
#import <scanner/scanner-Swift.h>

@implementation ScannerPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftScannerPlugin registerWithRegistrar:registrar];
}
@end
