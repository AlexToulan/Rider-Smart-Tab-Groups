package com.toucan_software.autotabgrouper

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.vfs.VirtualFile

class FileOpenedListener : FileEditorManagerListener {

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        val project = source.project
        val newFileExtension = file.extension?.toLowerCase() ?: return // Ignore files with no extension

        val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
        val allWindows = fileEditorManager.windows

        // Don't do anything if there's only one tab group open.
        if (allWindows.size <= 1) return

        var bestWindow: EditorWindow? = null
        var maxCount = 0

        // Find the window with the most files of the same type.
        for (window in allWindows) {
            val count = window.fileList.count { it.extension?.toLowerCase() == newFileExtension }
            if (count > maxCount) {
                maxCount = count
                bestWindow = window
            }
        }

        // If we found a suitable window with at least one matching file...
        if (bestWindow != null && maxCount > 0) {
            val currentWindow = fileEditorManager.currentWindow
            // ... and it's not the one the file is already in...
            if (bestWindow != currentWindow) {
                // Schedule the move to happen after the IDE is done opening the file.
                ApplicationManager.getApplication().invokeLater {
                    // This call will move the tab to the 'bestWindow'.
                    fileEditorManager.openFile(file, bestWindow!!)
                }
            }
        }
    }
}
