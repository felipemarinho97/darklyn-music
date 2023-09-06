package com.cappielloantonio.tempo.ui.fragment;

import android.content.ComponentName;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.util.RepeatModeUtil;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaBrowser;
import androidx.media3.session.SessionToken;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager2.widget.ViewPager2;

import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.databinding.InnerFragmentPlayerControllerBinding;
import com.cappielloantonio.tempo.service.MediaService;
import com.cappielloantonio.tempo.ui.activity.MainActivity;
import com.cappielloantonio.tempo.ui.dialog.RatingDialog;
import com.cappielloantonio.tempo.ui.fragment.pager.PlayerControllerHorizontalPager;
import com.cappielloantonio.tempo.util.Constants;
import com.cappielloantonio.tempo.util.MusicUtil;
import com.cappielloantonio.tempo.util.Preferences;
import com.cappielloantonio.tempo.viewmodel.PlayerBottomSheetViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.elevation.SurfaceColors;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

@UnstableApi
public class PlayerControllerFragment extends Fragment {
    private static final String TAG = "PlayerCoverFragment";

    private InnerFragmentPlayerControllerBinding bind;
    private ViewPager2 playerMediaCoverViewPager;
    private ToggleButton buttonFavorite;
    private TextView playerMediaTitleLabel;
    private TextView playerArtistNameLabel;
    private Button playbackSpeedButton;
    private ToggleButton skipSilenceToggleButton;
    private Chip playerMediaExtension;
    private TextView playerMediaBitrate;
    private ImageView playerMediaTranscodingIcon;
    private ImageView playerMediaTranscodingPriorityIcon;
    private Chip playerMediaTranscodedExtension;
    private TextView playerMediaTranscodedBitrate;
    private ConstraintLayout playerQuickActionView;
    private ImageButton playerOpenQueueButton;

    private MainActivity activity;
    private PlayerBottomSheetViewModel playerBottomSheetViewModel;
    private ListenableFuture<MediaBrowser> mediaBrowserListenableFuture;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();

        bind = InnerFragmentPlayerControllerBinding.inflate(inflater, container, false);
        View view = bind.getRoot();

        playerBottomSheetViewModel = new ViewModelProvider(requireActivity()).get(PlayerBottomSheetViewModel.class);

        init();
        initQuickActionView();
        initCoverLyricsSlideView();
        initMediaListenable();
        initArtistLabelButton();
        initTranscodingInfo();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        initializeBrowser();
        bindMediaController();
    }

    @Override
    public void onStop() {
        releaseBrowser();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }

    private void init() {
        playerMediaCoverViewPager = bind.getRoot().findViewById(R.id.player_media_cover_view_pager);
        buttonFavorite = bind.getRoot().findViewById(R.id.button_favorite);
        playerMediaTitleLabel = bind.getRoot().findViewById(R.id.player_media_title_label);
        playerArtistNameLabel = bind.getRoot().findViewById(R.id.player_artist_name_label);
        playbackSpeedButton = bind.getRoot().findViewById(R.id.player_playback_speed_button);
        skipSilenceToggleButton = bind.getRoot().findViewById(R.id.player_skip_silence_toggle_button);
        playerMediaExtension = bind.getRoot().findViewById(R.id.player_media_extension);
        playerMediaBitrate = bind.getRoot().findViewById(R.id.player_media_bitrate);
        playerMediaTranscodingIcon = bind.getRoot().findViewById(R.id.player_media_transcoding_audio);
        playerMediaTranscodingPriorityIcon = bind.getRoot().findViewById(R.id.player_media_server_transcode_priority);
        playerMediaTranscodedExtension = bind.getRoot().findViewById(R.id.player_media_transcoded_extension);
        playerMediaTranscodedBitrate = bind.getRoot().findViewById(R.id.player_media_transcoded_bitrate);
        playerQuickActionView = bind.getRoot().findViewById(R.id.player_quick_action_view);
        playerOpenQueueButton = bind.getRoot().findViewById(R.id.player_open_queue_button);
    }

    private void initQuickActionView() {
        playerQuickActionView.setBackgroundColor(SurfaceColors.getColorForElevation(requireContext(), 8));

        playerOpenQueueButton.setOnClickListener(view -> {
            PlayerBottomSheetFragment playerBottomSheetFragment = (PlayerBottomSheetFragment) requireActivity().getSupportFragmentManager().findFragmentByTag("PlayerBottomSheet");
            if (playerBottomSheetFragment != null) {
                playerBottomSheetFragment.goToQueuePage();
            }
        });
    }

    private void initializeBrowser() {
        mediaBrowserListenableFuture = new MediaBrowser.Builder(requireContext(), new SessionToken(requireContext(), new ComponentName(requireContext(), MediaService.class))).buildAsync();
    }

    private void releaseBrowser() {
        MediaBrowser.releaseFuture(mediaBrowserListenableFuture);
    }

    private void bindMediaController() {
        mediaBrowserListenableFuture.addListener(() -> {
            try {
                MediaBrowser mediaBrowser = mediaBrowserListenableFuture.get();

                bind.nowPlayingMediaControllerView.setPlayer(mediaBrowser);

                setMediaControllerListener(mediaBrowser);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, MoreExecutors.directExecutor());
    }

    private void setMediaControllerListener(MediaBrowser mediaBrowser) {
        setMediaControllerUI(mediaBrowser);
        setMetadata(mediaBrowser.getMediaMetadata());
        setMediaInfo(mediaBrowser.getMediaMetadata());

        mediaBrowser.addListener(new Player.Listener() {
            @Override
            public void onMediaMetadataChanged(@NonNull MediaMetadata mediaMetadata) {
                setMediaControllerUI(mediaBrowser);
                setMetadata(mediaMetadata);
                setMediaInfo(mediaMetadata);
            }
        });
    }

    private void setMetadata(MediaMetadata mediaMetadata) {
        playerMediaTitleLabel.setText(MusicUtil.getReadableString(String.valueOf(mediaMetadata.title)));
        playerArtistNameLabel.setText(MusicUtil.getReadableString(String.valueOf(mediaMetadata.artist)));

        playerMediaTitleLabel.setSelected(true);
        playerArtistNameLabel.setSelected(true);
    }

    private void setMediaInfo(MediaMetadata mediaMetadata) {
        if (mediaMetadata.extras != null) {
            String extension = mediaMetadata.extras.getString("suffix", "Unknown format");
            String bitrate = mediaMetadata.extras.getInt("bitrate", 0) != 0 ? mediaMetadata.extras.getInt("bitrate", 0) + "kbps" : "Original";

            playerMediaExtension.setText(extension);

            if (bitrate.equals("Original")) {
                playerMediaBitrate.setVisibility(View.GONE);
            } else {
                playerMediaBitrate.setVisibility(View.VISIBLE);
                playerMediaBitrate.setText(bitrate);
            }
        }

        String transcodingExtension = MusicUtil.getTranscodingFormatPreference();
        String transcodingBitrate = Integer.parseInt(MusicUtil.getBitratePreference()) != 0 ? Integer.parseInt(MusicUtil.getBitratePreference()) + "kbps" : "Original";

        if (transcodingExtension.equals("raw") && transcodingBitrate.equals("Original")) {
            playerMediaTranscodingPriorityIcon.setVisibility(View.GONE);
            playerMediaTranscodingIcon.setVisibility(View.GONE);
            playerMediaTranscodedBitrate.setVisibility(View.GONE);
            playerMediaTranscodedExtension.setVisibility(View.GONE);
        } else {
            playerMediaTranscodingPriorityIcon.setVisibility(View.GONE);
            playerMediaTranscodingIcon.setVisibility(View.VISIBLE);
            playerMediaTranscodedBitrate.setVisibility(View.VISIBLE);
            playerMediaTranscodedExtension.setVisibility(View.VISIBLE);
            playerMediaTranscodedExtension.setText(transcodingExtension);
            playerMediaTranscodedBitrate.setText(transcodingBitrate);
        }

        if (mediaMetadata.extras != null && mediaMetadata.extras.getString("uri", "").contains(Constants.DOWNLOAD_URI)) {
            playerMediaTranscodingPriorityIcon.setVisibility(View.GONE);
            playerMediaTranscodingIcon.setVisibility(View.GONE);
            playerMediaTranscodedBitrate.setVisibility(View.GONE);
            playerMediaTranscodedExtension.setVisibility(View.GONE);
        }

        if (Preferences.isServerPrioritized() && mediaMetadata.extras != null && !mediaMetadata.extras.getString("uri", "").contains(Constants.DOWNLOAD_URI)) {
            playerMediaTranscodingPriorityIcon.setVisibility(View.VISIBLE);
            playerMediaTranscodingIcon.setVisibility(View.GONE);
            playerMediaTranscodedBitrate.setVisibility(View.GONE);
            playerMediaTranscodedExtension.setVisibility(View.GONE);
        }
    }

    private void setMediaControllerUI(MediaBrowser mediaBrowser) {
        initPlaybackSpeedButton(mediaBrowser);

        if (mediaBrowser.getMediaMetadata().extras != null) {
            switch (mediaBrowser.getMediaMetadata().extras.getString("type", Constants.MEDIA_TYPE_MUSIC)) {
                case Constants.MEDIA_TYPE_PODCAST:
                    bind.getRoot().setShowShuffleButton(false);
                    bind.getRoot().setShowRewindButton(true);
                    bind.getRoot().setShowPreviousButton(false);
                    bind.getRoot().setShowNextButton(false);
                    bind.getRoot().setShowFastForwardButton(true);
                    bind.getRoot().setRepeatToggleModes(RepeatModeUtil.REPEAT_TOGGLE_MODE_NONE);
                    bind.getRoot().findViewById(R.id.player_playback_speed_button).setVisibility(View.VISIBLE);
                    bind.getRoot().findViewById(R.id.player_skip_silence_toggle_button).setVisibility(View.VISIBLE);
                    setPlaybackParameters(mediaBrowser);
                    break;
                case Constants.MEDIA_TYPE_RADIO:
                    bind.getRoot().setShowShuffleButton(false);
                    bind.getRoot().setShowRewindButton(false);
                    bind.getRoot().setShowPreviousButton(false);
                    bind.getRoot().setShowNextButton(false);
                    bind.getRoot().setShowFastForwardButton(false);
                    bind.getRoot().setRepeatToggleModes(RepeatModeUtil.REPEAT_TOGGLE_MODE_NONE);
                    bind.getRoot().findViewById(R.id.player_playback_speed_button).setVisibility(View.GONE);
                    bind.getRoot().findViewById(R.id.player_skip_silence_toggle_button).setVisibility(View.GONE);
                    setPlaybackParameters(mediaBrowser);
                    break;
                case Constants.MEDIA_TYPE_MUSIC:
                default:
                    bind.getRoot().setShowShuffleButton(true);
                    bind.getRoot().setShowRewindButton(false);
                    bind.getRoot().setShowPreviousButton(true);
                    bind.getRoot().setShowNextButton(true);
                    bind.getRoot().setShowFastForwardButton(false);
                    bind.getRoot().setRepeatToggleModes(RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL | RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE);
                    bind.getRoot().findViewById(R.id.player_playback_speed_button).setVisibility(View.GONE);
                    bind.getRoot().findViewById(R.id.player_skip_silence_toggle_button).setVisibility(View.GONE);
                    resetPlaybackParameters(mediaBrowser);
                    break;
            }
        }
    }

    private void initCoverLyricsSlideView() {
        playerMediaCoverViewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        playerMediaCoverViewPager.setAdapter(new PlayerControllerHorizontalPager(this));

        playerMediaCoverViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                if (position == 0) {
                    activity.setBottomSheetDraggableState(true);
                } else if (position == 1) {
                    activity.setBottomSheetDraggableState(false);
                }
            }
        });
    }

    private void initMediaListenable() {
        playerBottomSheetViewModel.getLiveMedia().observe(getViewLifecycleOwner(), media -> {
            if (media != null) {
                buttonFavorite.setChecked(media.getStarred() != null);
                buttonFavorite.setOnClickListener(v -> playerBottomSheetViewModel.setFavorite(requireContext(), media));
                buttonFavorite.setOnLongClickListener(v -> {
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(Constants.TRACK_OBJECT, media);

                    RatingDialog dialog = new RatingDialog();
                    dialog.setArguments(bundle);
                    dialog.show(requireActivity().getSupportFragmentManager(), null);

                    return true;
                });

                if (getActivity() != null) {
                    playerBottomSheetViewModel.refreshMediaInfo(requireActivity(), media);
                }
            }
        });
    }

    private void initArtistLabelButton() {
        playerBottomSheetViewModel.getLiveArtist().observe(getViewLifecycleOwner(), artist -> {
            if (artist != null) {
                playerArtistNameLabel.setOnClickListener(view -> {
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(Constants.ARTIST_OBJECT, artist);
                    NavHostFragment.findNavController(this).navigate(R.id.artistPageFragment, bundle);
                    activity.collapseBottomSheet();
                });
            }
        });
    }

    private void initTranscodingInfo() {
        playerMediaTranscodingPriorityIcon.setOnLongClickListener(view -> {
            Toast.makeText(requireActivity(), R.string.settings_audio_transcode_priority_toast, Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    private void initPlaybackSpeedButton(MediaBrowser mediaBrowser) {
        playbackSpeedButton.setOnClickListener(view -> {
            float currentSpeed = Preferences.getPlaybackSpeed();

            if (currentSpeed == Constants.MEDIA_PLAYBACK_SPEED_080) {
                mediaBrowser.setPlaybackParameters(new PlaybackParameters(Constants.MEDIA_PLAYBACK_SPEED_100));
                playbackSpeedButton.setText(getString(R.string.player_playback_speed, Constants.MEDIA_PLAYBACK_SPEED_100));
                Preferences.setPlaybackSpeed(Constants.MEDIA_PLAYBACK_SPEED_100);
            } else if (currentSpeed == Constants.MEDIA_PLAYBACK_SPEED_100) {
                mediaBrowser.setPlaybackParameters(new PlaybackParameters(Constants.MEDIA_PLAYBACK_SPEED_125));
                playbackSpeedButton.setText(getString(R.string.player_playback_speed, Constants.MEDIA_PLAYBACK_SPEED_125));
                Preferences.setPlaybackSpeed(Constants.MEDIA_PLAYBACK_SPEED_125);
            } else if (currentSpeed == Constants.MEDIA_PLAYBACK_SPEED_125) {
                mediaBrowser.setPlaybackParameters(new PlaybackParameters(Constants.MEDIA_PLAYBACK_SPEED_150));
                playbackSpeedButton.setText(getString(R.string.player_playback_speed, Constants.MEDIA_PLAYBACK_SPEED_150));
                Preferences.setPlaybackSpeed(Constants.MEDIA_PLAYBACK_SPEED_150);
            } else if (currentSpeed == Constants.MEDIA_PLAYBACK_SPEED_150) {
                mediaBrowser.setPlaybackParameters(new PlaybackParameters(Constants.MEDIA_PLAYBACK_SPEED_175));
                playbackSpeedButton.setText(getString(R.string.player_playback_speed, Constants.MEDIA_PLAYBACK_SPEED_175));
                Preferences.setPlaybackSpeed(Constants.MEDIA_PLAYBACK_SPEED_175);
            } else if (currentSpeed == Constants.MEDIA_PLAYBACK_SPEED_175) {
                mediaBrowser.setPlaybackParameters(new PlaybackParameters(Constants.MEDIA_PLAYBACK_SPEED_200));
                playbackSpeedButton.setText(getString(R.string.player_playback_speed, Constants.MEDIA_PLAYBACK_SPEED_200));
                Preferences.setPlaybackSpeed(Constants.MEDIA_PLAYBACK_SPEED_200);
            } else if (currentSpeed == Constants.MEDIA_PLAYBACK_SPEED_200) {
                mediaBrowser.setPlaybackParameters(new PlaybackParameters(Constants.MEDIA_PLAYBACK_SPEED_080));
                playbackSpeedButton.setText(getString(R.string.player_playback_speed, Constants.MEDIA_PLAYBACK_SPEED_080));
                Preferences.setPlaybackSpeed(Constants.MEDIA_PLAYBACK_SPEED_080);
            }
        });

        skipSilenceToggleButton.setOnClickListener(view -> {
            Preferences.setSkipSilenceMode(!skipSilenceToggleButton.isChecked());
        });
    }

    public void goToControllerPage() {
        playerMediaCoverViewPager.setCurrentItem(0, false);
    }

    public void goToLyricsPage() {
        playerMediaCoverViewPager.setCurrentItem(1, true);
    }

    private void setPlaybackParameters(MediaBrowser mediaBrowser) {
        Button playbackSpeedButton = bind.getRoot().findViewById(R.id.player_playback_speed_button);
        float currentSpeed = Preferences.getPlaybackSpeed();
        boolean skipSilence = Preferences.isSkipSilenceMode();

        mediaBrowser.setPlaybackParameters(new PlaybackParameters(currentSpeed));
        playbackSpeedButton.setText(getString(R.string.player_playback_speed, currentSpeed));

        // TODO Skippare il silenzio
        skipSilenceToggleButton.setChecked(skipSilence);
    }

    private void resetPlaybackParameters(MediaBrowser mediaBrowser) {
        mediaBrowser.setPlaybackParameters(new PlaybackParameters(Constants.MEDIA_PLAYBACK_SPEED_100));
        // TODO Resettare lo skip del silenzio
    }
}