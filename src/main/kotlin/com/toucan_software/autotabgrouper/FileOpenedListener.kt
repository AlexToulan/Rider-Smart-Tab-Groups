package com.toucan_software.autotabgrouper

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.diagnostic.Logger

class FileOpenedListener : FileEditorManagerListener {

    companion object {
        private val LOG = Logger.getInstance(FileOpenedListener::class.java)

        // Helper function to get extension counts for a window
        private fun getExtensionCounts(window: EditorWindow, excludeFile: VirtualFile? = null): MutableMap<String, Int> {
            val counts = mutableMapOf<String, Int>()
            for (f in window.fileList) {
                if (f == excludeFile) continue
                val ext = f.extension?.toLowerCase() ?: continue
                counts[ext] = (counts[ext] ?: 0) + 1
            }
            return counts
        }
    }

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        LOG.info("FileOpenedListener: fileOpened method called for file: ${file.name}")
        val project = source.project
        val newFileExtension = file.extension?.toLowerCase() ?: run {
            LOG.info("FileOpenedListener: File has no extension or extension is empty. Ignoring: ${file.name}")
            return
        }

        val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
        val allWindows = fileEditorManager.windows

        LOG.info("FileOpenedListener: Found ${allWindows.size} editor windows.")

        // Don't do anything if there's only one tab group open.
        if (allWindows.size <= 1) {
            LOG.info("FileOpenedListener: Only one window open. No grouping needed.")
            return
        }

        var bestWindow: EditorWindow? = null
        var maxCount = 0

        // Find the window with the most files of the same type.
        for (window in allWindows) {
            val countsForWindow = getExtensionCounts(window, file) // Exclude the current file
            val count = countsForWindow[newFileExtension] ?: 0
            LOG.info("FileOpenedListener: Window ${window.toString()} has $count files with extension .$newFileExtension (from dictionary)")
            if (count > maxCount) {
                maxCount = count
                bestWindow = window
            }
        }

        // If we found a suitable window with at least one matching file...
        if (bestWindow != null && maxCount > 0) {
            val currentWindow = fileEditorManager.currentWindow
            LOG.info("FileOpenedListener: Best window found: ${bestWindow.toString()} with $maxCount matches. Current window: ${currentWindow?.toString()}")
            // ... and it's not the one the file is already in...
            if (bestWindow != currentWindow) {
                LOG.info("FileOpenedListener: Scheduling move of ${file.name} to window ${bestWindow.toString()}")
                // Explicitly close the file from the current window first
                currentWindow?.closeFile(file)
                // Schedule the move to happen after the IDE is done opening the file.
                ApplicationManager.getApplication().invokeLater {
                    // This call will move the tab to the 'bestWindow'.
                    fileEditorManager.openFile(file, bestWindow!!)
                    LOG.info("FileOpenedListener: Move of ${file.name} completed to window ${bestWindow.toString()}")
                    LOG.info("FileOpenedListener: State after move - Best window files: ${bestWindow.fileList.map { it.name }}")
                    LOG.info("FileOpenedListener: State after move - Current window files: ${currentWindow?.fileList?.map { it.name }}")
                }
            } else {
                LOG.info("FileOpenedListener: File ${file.name} is already in the best window. No move needed.")
            }
        } else {
            LOG.info("FileOpenedListener: No suitable window found for ${file.name} with extension .$newFileExtension. No grouping applied.")
        }
    }
}
