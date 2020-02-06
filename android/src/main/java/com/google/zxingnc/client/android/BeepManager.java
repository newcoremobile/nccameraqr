/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxingnc.client.android;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;

import xinheyun.com.scanner.R;

/**
 * Manages beeps and vibrations for {@link CaptureActivity}.
 */
public class BeepManager implements
    MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, Closeable {

  private static final String TAG = BeepManager.class.getSimpleName();

  private static final float BEEP_VOLUME = 0.10f;
  private static final long VIBRATE_DURATION = 200L;

  private final Context context;
  private MediaPlayer mediaPlayer;

  public BeepManager(Context context) {
    this.context = context;
    this.mediaPlayer = null;
    updatePrefs();
  }

  synchronized void updatePrefs() {
    if (mediaPlayer == null) {
      // The volume on STREAM_SYSTEM is not adjustable, and users found it too loud,
      // so we now play on the music stream.

      mediaPlayer = buildMediaPlayer(context);
    }
  }

  public synchronized void playBeepSoundAndVibrate() {
    if (mediaPlayer != null) {
      mediaPlayer.start();
    }

    Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    vibrator.vibrate(VIBRATE_DURATION);

  }

  private MediaPlayer buildMediaPlayer(Context activity) {
    MediaPlayer mediaPlayer = new MediaPlayer();
//    mediaPlayer.setOnCompletionListener(this);
    try {
      AssetFileDescriptor file = activity.getResources().openRawResourceFd(R.raw.beep);
      try {
        mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
      } finally {
        file.close();
      }
      mediaPlayer.setLooping(false);
      mediaPlayer.setOnErrorListener(this);
      mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
      mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
      mediaPlayer.prepare();
      return mediaPlayer;
    } catch (IOException ioe) {
      Log.w(TAG, ioe);
      mediaPlayer.release();
      return null;
    }
  }

  @Override
  public void onCompletion(MediaPlayer mp) {
    // When the beep has finished playing, rewind to queue up another one.      
    mp.seekTo(0);
  }

  @Override
  public synchronized boolean onError(MediaPlayer mp, int what, int extra) {
      // possibly media player error, so release and recreate
    close();
    return true;
  }

  @Override
  public synchronized void close() {
    if (mediaPlayer != null) {
      mediaPlayer.release();
      mediaPlayer = null;
    }
  }

}