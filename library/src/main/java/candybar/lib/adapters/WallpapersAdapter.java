package candybar.lib.adapters;

import android.animation.AnimatorInflater;
import android.animation.StateListAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.danimahardhika.android.helpers.core.ColorHelper;
import com.danimahardhika.android.helpers.permission.PermissionHelper;
import com.kogitune.activitytransition.ActivityTransitionLauncher;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.List;

import candybar.lib.R;
import candybar.lib.activities.CandyBarWallpaperActivity;
import candybar.lib.applications.CandyBarApplication;
import candybar.lib.databases.Database;
import candybar.lib.helpers.ViewHelper;
import candybar.lib.items.PopupItem;
import candybar.lib.items.Wallpaper;
import candybar.lib.preferences.Preferences;
import candybar.lib.tasks.WallpaperApplyTask;
import candybar.lib.utils.Extras;
import candybar.lib.utils.ImageConfig;
import candybar.lib.utils.Popup;
import candybar.lib.utils.WallpaperDownloader;
import candybar.lib.utils.views.HeaderView;

/*
 * CandyBar - Material Dashboard
 *
 * Copyright (c) 2014-2016 Dani Mahardhika
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

public class WallpapersAdapter extends RecyclerView.Adapter<WallpapersAdapter.ViewHolder> {

    private final Context mContext;
    private final List<Wallpaper> mWallpapers;
    private final DisplayImageOptions.Builder mOptions;

    public static boolean sIsClickable = true;
    private final boolean mIsAutoGeneratedColor;
    private final boolean mIsShowName;

    public WallpapersAdapter(@NonNull Context context, @NonNull List<Wallpaper> wallpapers) {
        mContext = context;
        mWallpapers = wallpapers;
        mIsAutoGeneratedColor = mContext.getResources().getBoolean(
                R.bool.card_wallpaper_auto_generated_color);
        mIsShowName = mContext.getResources().getBoolean(R.bool.wallpaper_show_name_author);

        mOptions = ImageConfig.getRawDefaultImageOptions();
        mOptions.resetViewBeforeLoading(true);
        mOptions.cacheInMemory(true);
        mOptions.cacheOnDisk(true);
        mOptions.displayer(new FadeInBitmapDisplayer(700));
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(
                R.layout.fragment_wallpapers_item_grid_alt, parent, false);
        if (mIsShowName) {
            view = LayoutInflater.from(mContext).inflate(
                    R.layout.fragment_wallpapers_item_grid, parent, false);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Wallpaper wallpaper = mWallpapers.get(position);
        if (mIsShowName) {
            holder.name.setText(wallpaper.getName());
            holder.author.setText(wallpaper.getAuthor());
        }

        ImageLoader.getInstance().displayImage(wallpaper.getThumbUrl(), new ImageViewAware(holder.image),
                mOptions.build(), ImageConfig.getThumbnailSize(), new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingStarted(String imageUri, View view) {
                        super.onLoadingStarted(imageUri, view);
                        if (mIsAutoGeneratedColor && mIsShowName) {
                            int color = wallpaper.getColor();
                            if (color == 0) {
                                color = ColorHelper.getAttributeColor(
                                        mContext, R.attr.card_background);
                            }

                            int text = ColorHelper.getTitleTextColor(color);
                            holder.name.setTextColor(text);
                            holder.author.setTextColor(ColorHelper.getBodyTextColor(color));
                            holder.card.setCardBackgroundColor(color);
                        }
                    }

                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        super.onLoadingComplete(imageUri, view, loadedImage);
                        if (mIsAutoGeneratedColor && mIsShowName && loadedImage != null && wallpaper.getColor() == 0) {
                            Palette.from(loadedImage).generate(palette -> {
                                int defaultColor = ColorHelper.getAttributeColor(
                                        mContext, R.attr.card_background);
                                int color = palette.getVibrantColor(defaultColor);
                                if (color == defaultColor)
                                    color = palette.getMutedColor(defaultColor);

                                holder.card.setCardBackgroundColor(color);
                                int text = ColorHelper.getTitleTextColor(color);
                                holder.name.setTextColor(text);
                                holder.author.setTextColor(ColorHelper.getBodyTextColor(color));

                                wallpaper.setColor(color);
                                Database.get(mContext).updateWallpaper(wallpaper);
                            });
                        }
                    }
                }, null);
    }

    @Override
    public int getItemCount() {
        return mWallpapers.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            View.OnLongClickListener {

        private final CardView card;
        private final HeaderView image;
        private TextView name;
        private TextView author;

        ViewHolder(View itemView) {
            super(itemView);
            String viewStyle = mContext.getResources().getString(
                    R.string.wallpaper_grid_preview_style);
            Point ratio = ViewHelper.getWallpaperViewRatio(viewStyle);

            image = itemView.findViewById(R.id.image);
            image.setRatio(ratio.x, ratio.y);

            card = itemView.findViewById(R.id.card);
            if (CandyBarApplication.getConfiguration().getWallpapersGrid() == CandyBarApplication.GridStyle.FLAT) {
                if (card.getLayoutParams() instanceof GridLayoutManager.LayoutParams) {
                    card.setRadius(0f);
                    card.setUseCompatPadding(false);
                    int margin = mContext.getResources().getDimensionPixelSize(R.dimen.card_margin);
                    GridLayoutManager.LayoutParams params = (GridLayoutManager.LayoutParams) card.getLayoutParams();
                    params.setMargins(0, 0, margin, margin);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        params.setMarginEnd(margin);
                    }
                }
            }

            if (!Preferences.get(mContext).isCardShadowEnabled()) {
                card.setCardElevation(0);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                StateListAnimator stateListAnimator = AnimatorInflater
                        .loadStateListAnimator(mContext, R.animator.card_lift);
                card.setStateListAnimator(stateListAnimator);
            }

            if (mIsShowName) {
                name = itemView.findViewById(R.id.name);
                author = itemView.findViewById(R.id.author);
            }

            card.setOnClickListener(this);
            card.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int id = view.getId();
            int position = getAdapterPosition();
            if (id == R.id.card) {
                if (sIsClickable) {
                    sIsClickable = false;
                    try {
                        Bitmap bitmap = null;
                        if (image.getDrawable() != null) {
                            bitmap = ((BitmapDrawable) image.getDrawable()).getBitmap();
                        }

                        final Intent intent = new Intent(mContext, CandyBarWallpaperActivity.class);
                        intent.putExtra(Extras.EXTRA_URL, mWallpapers.get(position).getURL());

                        ActivityTransitionLauncher.with((AppCompatActivity) mContext)
                                .from(image, Extras.EXTRA_IMAGE)
                                .image(bitmap)
                                .launch(intent);
                    } catch (Exception e) {
                        sIsClickable = true;
                    }
                }
            }
        }

        @Override
        public boolean onLongClick(View view) {
            int id = view.getId();
            int position = getAdapterPosition();
            if (id == R.id.card) {
                if (position < 0 || position > mWallpapers.size()) {
                    return false;
                }

                Popup popup = Popup.Builder(mContext)
                        .to(name != null ? name : view)
                        .list(PopupItem.getApplyItems(mContext))
                        .callback((applyPopup, i) -> {
                            PopupItem item = applyPopup.getItems().get(i);
                            if (item.getType() == PopupItem.Type.WALLPAPER_CROP) {
                                Preferences.get(mContext).setCropWallpaper(!item.getCheckboxValue());
                                item.setCheckboxValue(Preferences.get(mContext).isCropWallpaper());

                                applyPopup.updateItem(i, item);
                                return;
                            } else if (item.getType() == PopupItem.Type.DOWNLOAD) {
                                if (PermissionHelper.isStorageGranted(mContext)) {
                                    WallpaperDownloader.prepare(mContext)
                                            .wallpaper(mWallpapers.get(position))
                                            .start();
                                } else {
                                    PermissionHelper.requestStorage(mContext);
                                }
                            } else {
                                WallpaperApplyTask task = WallpaperApplyTask.prepare(mContext)
                                        .wallpaper(mWallpapers.get(position));

                                if (item.getType() == PopupItem.Type.LOCKSCREEN) {
                                    task.to(WallpaperApplyTask.Apply.LOCKSCREEN);
                                } else if (item.getType() == PopupItem.Type.HOMESCREEN) {
                                    task.to(WallpaperApplyTask.Apply.HOMESCREEN);
                                } else if (item.getType() == PopupItem.Type.HOMESCREEN_LOCKSCREEN) {
                                    task.to(WallpaperApplyTask.Apply.HOMESCREEN_LOCKSCREEN);
                                }

                                task.start(AsyncTask.THREAD_POOL_EXECUTOR);
                            }
                            applyPopup.dismiss();
                        })
                        .build();

                popup.show();
                return true;
            }
            return false;
        }
    }
}
