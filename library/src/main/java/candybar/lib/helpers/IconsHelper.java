package candybar.lib.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.danimahardhika.android.helpers.core.utils.LogUtil;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import candybar.lib.R;
import candybar.lib.activities.CandyBarMainActivity;
import candybar.lib.applications.CandyBarApplication;
import candybar.lib.fragments.dialog.IconPreviewFragment;
import candybar.lib.items.Icon;
import candybar.lib.preferences.Preferences;
import candybar.lib.utils.AlphanumComparator;
import candybar.lib.utils.ImageConfig;

import static candybar.lib.helpers.DrawableHelper.getRightIcon;
import static com.danimahardhika.android.helpers.core.DrawableHelper.getResourceId;
import static com.danimahardhika.android.helpers.core.FileHelper.getUriFromFile;

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

public class IconsHelper {

    @NonNull
    public static List<Icon> getIconsList(@NonNull Context context) throws Exception {
        XmlResourceParser parser = context.getResources().getXml(R.xml.drawable);
        int eventType = parser.getEventType();
        String sectionTitle = "";
        List<Icon> icons = new ArrayList<>();
        List<Icon> sections = new ArrayList<>();

        int count = 0;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("category")) {
                    String title = parser.getAttributeValue(null, "title");
                    if (!sectionTitle.equals(title)) {
                        if (sectionTitle.length() > 0) {
                            count += icons.size();
                            sections.add(new Icon(sectionTitle, icons));
                        }
                    }
                    sectionTitle = title;
                    icons = new ArrayList<>();
                } else if (parser.getName().equals("item")) {
                    String name = parser.getAttributeValue(null, "drawable");
                    String customName = parser.getAttributeValue(null, "name");
                    int id = getResourceId(context, name);
                    if (id > 0) {
                        icons.add(new Icon(name, customName, id));
                    }
                }
            }

            eventType = parser.next();
        }
        count += icons.size();
        CandyBarMainActivity.sIconsCount = count;
        if (!CandyBarApplication.getConfiguration().isAutomaticIconsCountEnabled() &&
                CandyBarApplication.getConfiguration().getCustomIconsCount() == 0) {
            CandyBarApplication.getConfiguration().setCustomIconsCount(count);
        }
        sections.add(new Icon(sectionTitle, icons));
        parser.close();
        return sections;
    }

    public static List<Icon> getTabAllIcons() {
        List<Icon> icons = new ArrayList<>();
        String[] categories = CandyBarApplication.getConfiguration().getCategoryForTabAllIcons();

        if (categories != null && categories.length > 0) {
            for (String category : categories) {
                for (Icon section : CandyBarMainActivity.sSections) {
                    if (section.getTitle().equals(category)) {
                        icons.addAll(section.getIcons());
                        break;
                    }
                }
            }
        } else {
            for (Icon section : CandyBarMainActivity.sSections) {
                icons.addAll(section.getIcons());
            }
        }

        Collections.sort(icons, new AlphanumComparator() {
            @Override
            public int compare(Object o1, Object o2) {
                String s1 = ((Icon) o1).getTitle();
                String s2 = ((Icon) o2).getTitle();
                return super.compare(s1, s2);
            }
        });
        return icons;
    }

    public static String replaceName(@NonNull Context context, boolean iconReplacer, String name) {
        if (iconReplacer) {
            String[] replacer = context.getResources().getStringArray(R.array.icon_name_replacer);
            for (String replace : replacer) {
                String[] strings = replace.split(",");
                if (strings.length > 0)
                    name = name.replace(strings[0], strings.length > 1 ? strings[1] : "");
            }
        }
        name = name.replaceAll("_", " ");
        name = name.trim().replaceAll("\\s+", " ");
        return capitalizeWord(name);
    }

    public static String capitalizeWord(String str) {
        String[] words = str.split("\\s");
        String capitalizeWord = "";
        for (String w : words) {
            String first = w.substring(0, 1);
            String afterfirst = w.substring(1);
            capitalizeWord += first.toUpperCase() + afterfirst + " ";
        }
        return capitalizeWord.trim();
    }

    public static void selectIcon(@NonNull Context context, int action, Icon icon) {
        if (action == IntentHelper.ICON_PICKER) {
            Intent intent = new Intent();
            ImageLoader.setShape(Preferences.get(context).getIconShape());
            Bitmap bitmap = ImageLoader.getInstance().loadImageSync(
                    "drawable://" + icon.getRes(), ImageConfig.getRawImageOptions().build());

            intent.putExtra("icon", bitmap);
            ((AppCompatActivity) context).setResult(bitmap != null ?
                    Activity.RESULT_OK : Activity.RESULT_CANCELED, intent);
            ((AppCompatActivity) context).finish();
        } else if (action == IntentHelper.IMAGE_PICKER) {
            Intent intent = new Intent();
            ImageLoader.setShape(Preferences.get(context).getIconShape());
            Bitmap bitmap = ImageLoader.getInstance().loadImageSync(
                    "drawable://" + icon.getRes(), ImageConfig.getRawImageOptions().build());
            if (bitmap != null) {
                File file = new File(context.getCacheDir(), icon.getTitle() + ".png");
                FileOutputStream outStream;
                try {
                    outStream = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                    outStream.flush();
                    outStream.close();

                    Uri uri = getUriFromFile(context, context.getPackageName(), file);
                    if (uri == null) uri = Uri.fromFile(file);
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    intent.setData(uri);
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception | OutOfMemoryError e) {
                    LogUtil.e(Log.getStackTraceString(e));
                }
                intent.putExtra("return-data", false);
            }
            ((AppCompatActivity) context).setResult(bitmap != null ?
                    Activity.RESULT_OK : Activity.RESULT_CANCELED, intent);
            ((AppCompatActivity) context).finish();
        } else {
            IconPreviewFragment.showIconPreview(((AppCompatActivity) context)
                            .getSupportFragmentManager(),
                    icon.getTitle(), icon.getRes());
        }
    }

    @Nullable
    public static String saveIcon(List<String> files, File directory, Drawable drawable, String name) {
        String fileName = name.toLowerCase().replaceAll(" ", "_") + ".png";
        File file = new File(directory, fileName);
        try {
            Thread.sleep(2);
            Bitmap bitmap = getRightIcon(drawable);

            if (files.contains(file.toString())) {
                fileName = fileName.replace(".png", "_" + System.currentTimeMillis() + ".png");
                file = new File(directory, fileName);

                LogUtil.e("Duplicate File name, Renamed: " + fileName);
            }

            FileOutputStream outStream = new FileOutputStream(file);
            // Disabled Because of OutOfMemory
            //bitmap = Bitmap.createScaledBitmap(bitmap, 192, 192, false);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.flush();
            outStream.close();
            return directory.toString() + "/" + fileName;
        } catch (Exception | OutOfMemoryError e) {
            LogUtil.e(Log.getStackTraceString(e));
        }
        return null;
    }

    @Nullable
    public static byte[] getByteFromDrawable(@Nullable Drawable drawable) {
        if (drawable != null) {
            Bitmap bitmap = getRightIcon(drawable);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, baos);
            byte[] data = baos.toByteArray();
            return data;
        }
        return null;
    }
}
