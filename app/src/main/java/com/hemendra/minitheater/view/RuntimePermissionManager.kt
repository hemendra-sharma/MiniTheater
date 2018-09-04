/*
 * Copyright (c) 2018 Hemendra Sharma
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hemendra.minitheater.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.support.v7.app.AppCompatActivity

import com.hemendra.minitheater.R
import android.support.v4.content.ContextCompat



internal class RuntimePermissionManager(private val activity: AppCompatActivity) {

    companion object {
        private const val REQUEST_READ_WRITE_PERMISSION = 1001
    }

    private fun hasReadWritePermissions(): Boolean {
        return ContextCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks and ask user to give required permissions.
     * @return TRUE is already has permissions. FALSE otherwise.
     */
    fun askForPermissions(): Boolean {
        if(hasReadWritePermissions())
            return true
        else {
            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (activity.shouldShowRequestPermissionRationale(permission)) {
                showMessage(activity,
                        activity.getString(R.string.permission_request_rationale),
                        Runnable {
                            activity.requestPermissions(arrayOf(permission),
                                    REQUEST_READ_WRITE_PERMISSION)
                        })
            } else {
                activity.requestPermissions(arrayOf(permission), REQUEST_READ_WRITE_PERMISSION)
            }
            return false
        }
    }

    fun onRequestPermissionsResult(requestCode: Int,
                                   permissions: Array<String>,
                                   grantResults: IntArray,
                                   onPermissionGranted: Runnable) {
        when (requestCode) {
            REQUEST_READ_WRITE_PERMISSION -> {
                if (permissions.isNotEmpty() && grantResults.isNotEmpty()) {
                    var allPermissionsGranted = true
                    for (result in grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            allPermissionsGranted = false
                        }
                    }
                    //
                    if (!allPermissionsGranted) {
                        showMessage(activity,
                                activity.getString(R.string.permission_request_from_app_settings),
                                Runnable { launchAppSettingsScreen() })
                    } else {
                        onPermissionGranted.run()
                    }
                } else {
                    // If request is cancelled, the result arrays are empty.
                    activity.finish()
                }
            }
        }
    }

    private fun launchAppSettingsScreen() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        intent.data = Uri.parse("package:" + activity.packageName)
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        activity.startActivity(intent)
        activity.finish()
    }

}
