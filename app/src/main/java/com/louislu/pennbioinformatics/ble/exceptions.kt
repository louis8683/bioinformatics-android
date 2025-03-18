package com.louislu.pennbioinformatics.ble

sealed class PermissionNotGrantedException(permission: String): Exception("Permission not granted: $permission")

class BleConnectPermissionNotGrantedException: PermissionNotGrantedException("BLE Connect")
class BleScanPermissionNotGrantedException: PermissionNotGrantedException("BLE Scan")
class CoarseLocationPermissionNotGrantedException: PermissionNotGrantedException("Coarse Location")
class FineLocationPermissionNotGrantedException: PermissionNotGrantedException("Fine Location")

class BleScannerNotAvailableException(): RuntimeException("BLE scanner not available")