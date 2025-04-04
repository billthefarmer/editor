package org.billthefarmer.editor;

import java.util.HashMap;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

public class SoundManager {
    private  SoundPool mSoundPool;
    private  HashMap<Integer, Integer> mSoundPoolMap;
    private  AudioManager  mAudioManager;
    private  Context mContext;


    public SoundManager()
    {

    }

    public void initSounds(Context theContext) {
        mContext = theContext;
        mSoundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);
        mSoundPoolMap = new HashMap<Integer, Integer>();
        mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
    }

    public void addSound(int Index,int SoundID)
    {
        mSoundPoolMap.put(1, mSoundPool.load(mContext, SoundID, 1));
    }

    public void playSound(int index) {

        int streamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mSoundPool.play(mSoundPoolMap.get(index), streamVolume, streamVolume, 1, 0, 1f);
    }

    public void playLoopedSound(int index) {

        int streamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mSoundPool.play(mSoundPoolMap.get(index), streamVolume, streamVolume, 1, -1, 1f);
    }
}
