package com.toucan_software.autotabgrouper

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.vfs.VirtualFile

class FileOpenedListener : FileEditorManagerListener {

    companion object {
        private val LOG = Logger.getInstance(FileOpenedListener::class.java)
    }

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        LOG.info("FileOpenedListener: fileOpened method called for file: ${file.name}")
        val project = source.project

        val selectedEditor = source.selectedEditor
        var caretOffset = -1
        if (selectedEditor is TextEditor && selectedEditor.file == file) {
            caretOffset = selectedEditor.editor.caretModel.offset
            LOG.info("FileOpenedListener: Captured caret offset $caretOffset for ${file.name}")
        }

        val newFileExtension = file.extension?.lowercase() ?: run {
            LOG.info("FileOpenedListener: File has no extension or extension is empty. Ignoring: ${file.name}")
            return
        }

        val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
        val allWindows = fileEditorManager.windows
        val currentWindow = fileEditorManager.currentWindow // Get current window here

        // Don't do anything if there's only one tab group open.
        if (allWindows.size <= 1) {
            LOG.info("FileOpenedListener: Only one window open. No grouping needed.")
            return
        }

        var bestWindow: EditorWindow? = null
        var maxCount = 0

        // Find the window with the most files of the same type.
        for (window in allWindows) {
            val count = window.fileList.count { it.extension?.lowercase() == newFileExtension && it != file }
            LOG.info("FileOpenedListener: Window ${window.toString()} has $count files with extension .$newFileExtension")
            if (count > maxCount) {
                maxCount = count
                bestWindow = window
            }
        }

        // If we found a suitable window with at least one matching file...
        if (bestWindow != null && maxCount > 0) {
            // ... and it's not the one the file is already in...
            if (bestWindow != currentWindow) {
                LOG.info("FileOpenedListener: Scheduling move of ${file.name} to window ${bestWindow.toString()}")
                // Explicitly close the file from the current window first
                currentWindow?.closeFile(file)
                // Schedule the move to happen after the IDE is done opening the file.
                val finalCaretOffset = caretOffset
                ApplicationManager.getApplication().invokeLater {
                    fileEditorManager.openFile(file, bestWindow!!)
                    if (finalCaretOffset != -1) {
                        ApplicationManager.getApplication().invokeLater {
                            val descriptor = OpenFileDescriptor(project, file, finalCaretOffset)
                            descriptor.navigate(true)
                            LOG.info("FileOpenedListener: Restored caret position to offset $finalCaretOffset for ${file.name}")
                        }
                    }
                    LOG.info("FileOpenedListener: Move of ${file.name} completed to window ${bestWindow.toString()}")
                }
            } else {
                LOG.info("FileOpenedListener: File ${file.name} is already in the best window. No move needed.")
            }
        } else {
            LOG.info("FileOpenedListener: No suitable window found for ${file.name} with extension .$newFileExtension. No grouping applied.")
        }
    }
}