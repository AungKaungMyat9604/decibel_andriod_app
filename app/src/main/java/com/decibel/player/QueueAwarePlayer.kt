package com.decibel.player

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player

/**
 * Exposes next/previous to System UI even when ExoPlayer only holds one MediaItem.
 * Skip actions are delegated to [PlaybackController]'s app-level queue.
 */
class QueueAwarePlayer(
    player: Player,
    private val onSkipNext: () -> Unit,
    private val onSkipPrevious: () -> Unit,
) : ForwardingPlayer(player) {
    override fun getAvailableCommands(): Player.Commands =
        super.getAvailableCommands()
            .buildUpon()
            .add(COMMAND_SEEK_TO_NEXT)
            .add(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .add(COMMAND_SEEK_TO_PREVIOUS)
            .add(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .build()

    override fun isCommandAvailable(command: @Player.Command Int): Boolean =
        when (command) {
            COMMAND_SEEK_TO_NEXT,
            COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            COMMAND_SEEK_TO_PREVIOUS,
            COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            -> true
            else -> super.isCommandAvailable(command)
        }

    override fun seekToNext() = onSkipNext()

    override fun seekToNextMediaItem() = onSkipNext()

    override fun seekToPrevious() = onSkipPrevious()

    override fun seekToPreviousMediaItem() = onSkipPrevious()
}
