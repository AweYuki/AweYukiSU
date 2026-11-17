// IKsuInterface.aidl
package com.hzzmonnet.aweyuki.rootService;

import android.content.pm.PackageInfo;
import java.util.List;

interface IKsuInterface {
    int getPackageCount();
    List<PackageInfo> getPackages(int start, int maxCount);
}