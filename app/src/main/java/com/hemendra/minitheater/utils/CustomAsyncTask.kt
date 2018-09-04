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

package com.hemendra.minitheater.utils

import android.os.Handler
import android.os.Message
import android.support.annotation.WorkerThread

/**
 * After observing several different issues with the Android's [android.os.AsyncTask],
 * I decided to create my own implementation of it which works in a similar way, so that,
 * replacing it in any old project is easy. We just need to change the class name.
 * @param <Params> Parameter type
 * @param <Progress> Progress type
 * @param <Result> Result type
</Result></Progress></Params> */
abstract class CustomAsyncTask<Params, Progress, Result> : Handler.Callback {

    private var isFinished = true
    private var res: Result? = null
    private var executingThread: Thread? = null
    /**
     * Check whether the task execution has been cancelled or not.
     * @return TRUE if cancelled, FALSE otherwise.
     */
    protected var isCancelled = false

    private val progressHandler = Handler(this)

    /**
     * Check whether the task execution is going on, or not.
     * @return TRUE if in progress, FALSE otherwise.
     */
    val isExecuting: Boolean = !isFinished

    override fun handleMessage(msg: Message?): Boolean {
        msg?.let {
            if (it.what == 1) {
                onProgressUpdate(*it.obj as Array<Progress>)
            } else if (it.what == 2) {
                if (!isCancelled)
                    onPostExecute(res)
                else
                    onCancelled()
            }
        }
        return true
    }

    /**
     * This method gets called right before the doInBackground starts executing.
     */
    open protected fun onPreExecute() {}

    /**
     * Call this method in the middle of task execution to request it to stop.
     * @param interrupt if TRUE then the worker thread will be interrupted.
     */
    open fun cancel(interrupt: Boolean) {
        isCancelled = true
        isFinished = true
        if (interrupt && executingThread != null)
            executingThread!!.interrupt()
    }

    /**
     * Call this method to invoke the "onProgressUpdate" method from main thread. This method is
     * typically used to update the UI elements.
     * @param progress Provide the object array of type 'Progress'.
     */
    fun publishProgress(vararg progress: Progress) {
        val msg = Message()
        msg.what = 1
        msg.obj = progress
        progressHandler.sendMessage(msg)
    }

    /**
     * This method is called when there is a call to "publishProgress" has been made.
     * @param progress Provides the object array of the type 'Progress'.
     */
    open protected fun onProgressUpdate(vararg progress: Progress) {}

    /**
     * The instructions defined in this method will be executed in a separate background (worker)
     * thread.
     * @param params Provides the object array of type 'Params'.
     * @return Object of type 'Result'.
     */
    @WorkerThread
    protected abstract fun doInBackground(vararg params: Params): Result

    /**
     * This method gets called when 'doInBackground' has been executed completely.
     * @param result Object of type 'Result' which was returned by 'doInBackground'.
     */
    open protected fun onPostExecute(result: Result?) {}

    /**
     * This method gets called when the execution was cancelled.
     */
    open protected fun onCancelled() {}

    /**
     * Start the execution
     * @param params Provide the parameters (if any), or leave empty otherwise.
     */
    @SafeVarargs
    fun execute(vararg params: Params) {
        isCancelled = false
        onPreExecute()
        //
        isFinished = false
        //
        executingThread = Thread {
            res = doInBackground(*params)
            isFinished = true
            val msg = Message()
            msg.what = 2
            progressHandler.sendMessage(msg)
        }
        executingThread!!.start()
    }

}
