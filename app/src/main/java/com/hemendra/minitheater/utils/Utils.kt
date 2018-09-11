package com.hemendra.minitheater.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Environment
import java.io.*

class Utils {

    companion object {

        fun isNetworkAvailable(context: Context): Boolean {
            val connectivity = context
                    .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            for (network in connectivity.allNetworks) {
                val info = connectivity.getNetworkInfo(network)
                if (info != null && info.state == NetworkInfo.State.CONNECTED)
                    return true
            }
            return false
        }

        fun readObjectFromFile(file: File): Any? {
            var instr: FileInputStream? = null
            var bufferIn: BufferedInputStream? = null
            var objIn: ObjectInput? = null
            try {
                if(!file.exists()) return null

                instr = FileInputStream(file)
                bufferIn = BufferedInputStream(instr)
                objIn = ObjectInputStream(bufferIn)
                return objIn.readObject()
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
                deleteFile(file)
            } catch (e: IOException) {
                e.printStackTrace()
            }finally {
                try {
                    //releasing the FileInputStream and ObjectInput
                    instr?.close()
                    bufferIn?.close()
                    objIn?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            return null
        }

        /**
         * Convert the given object into byte array.
         * Note: the object must implement [java.io.Serializable]
         * @param obj Object to convert into byte array.
         * @return Raw byte array.
         */
        private fun getSerializedData(obj: Any): ByteArray? {
            if(obj !is Serializable) return null

            val bos = ByteArrayOutputStream()
            var out: ObjectOutput? = null
            var bytes: ByteArray? = null
            try {
                out = ObjectOutputStream(bos)
                out.writeObject(obj)
                bytes = bos.toByteArray()
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                try {
                    bos.close()
                    out?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
            return bytes
        }

        /**
         * Convert the given object into byte array and write it to the given file path.
         * Note: the object must implement [java.io.Serializable]
         * @param obj Object that needs to be written to file.
         * @param file File target.
         * @return TRUE if the file was written successfully. FALSE otherwise.
         */
        fun writeToFile(obj: Any, file: File): Boolean {
            val data = getSerializedData(obj)
            return data != null && writeToFile(data, file)
        }

        /**
         * Write the raw byte array to the given file path.
         * @param data byte array to be written
         * @param file Target tile
         * @return TRUE if the file was written successfully. FALSE otherwise.
         */
        private fun writeToFile(data: ByteArray, file: File): Boolean {
            var fout: FileOutputStream? = null
            try {
                val proceed = file.parentFile.exists() || file.parentFile.mkdirs()
                if (proceed) {
                    if (file.exists() || file.createNewFile()) {
                        fout = FileOutputStream(file)
                        fout.write(data)
                        return true
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                try {
                    fout?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
            return false
        }

        /**
         * Delete the file at the given path
         * @param file target File
         * @return TRUE if deleted successfully, FALSE otherwise.
         */
        public fun deleteFile(file: File): Boolean {
            return file.exists() && file.delete()
        }

        /**
         * Delete the given directory and all its subdirectories
         * @param dir target directory
         * @return TRUE if deleted successfully, FALSE otherwise.
         */
        public fun deleteDirectory(dir: File): Boolean {
            var success = true
            if (dir.exists()) {
                val files = dir.listFiles()
                for (file in files) {
                    success = if (file.isDirectory) {
                        success and deleteDirectory(file)
                    } else {
                        success and file.delete()
                    }
                }
                success = success and dir.delete()
            }
            return success
        }

        fun getAvailableSpace(): Long = Environment.getExternalStorageDirectory().freeSpace
    }

}