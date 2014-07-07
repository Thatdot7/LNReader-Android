package com.erakk.lnreader.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.R;
import com.erakk.lnreader.UIHelper;
import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.PageModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PageModelAdapter extends ArrayAdapter<PageModel> {
    private static final String TAG = PageModelAdapter.class.toString();
    private final Context context;
    private int layoutResourceId;
    public List<PageModel> data;
    private PageModel[] originalData = new PageModel[0];

    public PageModelAdapter(Context context, int resourceId, List<PageModel> objects) {
        super(context, resourceId, objects);
        this.layoutResourceId = resourceId;
        this.context = context;
        this.data = objects;
        this.originalData = objects.toArray(originalData);
        filterData();
        Log.d(TAG, "created with " + objects.size() + " items");
    }

    public void setLayout(int resourceId) {
        this.layoutResourceId = resourceId;
    }

    @Override
    public void addAll(PageModel... objects) {
        synchronized (this) {
            if (data == null) {
                data = new ArrayList<PageModel>();
            }
            for (PageModel pageModel : objects) {
                data.add(pageModel);
            }
        }

        this.notifyDataSetChanged();
    }

    @Override
    public void addAll(Collection<? extends PageModel> objects) {
        synchronized (this) {
            if (data == null) {
                data = new ArrayList<PageModel>();
            }
            data.addAll(objects);
        }

        this.notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        PageModelHolder holder = null;

        final PageModel page = data.get(position);

        LayoutInflater inflater = ((Activity) context).getLayoutInflater();
        row = inflater.inflate(layoutResourceId, parent, false);
        holder = new PageModelHolder();
        holder.txtNovel = (TextView) row.findViewById(R.id.novel_name);
        if (holder.txtNovel != null) {
            holder.txtNovel.setText(page.getTitle());
            if (page.isHighlighted()) {
                holder.txtNovel.setTypeface(null, Typeface.BOLD);
                holder.txtNovel.setTextSize(20);
                holder.txtNovel.setText("?" + holder.txtNovel.getText());
            }

            if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Constants.PREF_INVERT_COLOR, true)) {
                holder.txtNovel.setTextColor(Constants.COLOR_UNREAD);
            } else {
                holder.txtNovel.setTextColor(Constants.COLOR_UNREAD_DARK);
            }
            if (page.isMissing()) {
                holder.txtNovel.setTextColor(Constants.COLOR_MISSING);
            }
            if (page.isRedlink()) {
                holder.txtNovel.setTextColor(Constants.COLOR_REDLINK);
            }
            if (page.isExternal()) {
                holder.txtNovel.setTextColor(Constants.COLOR_EXTERNAL);
            }
            ImageView ivExternal = (ImageView) row.findViewById(R.id.is_external);
            if (ivExternal != null) {
                if (page.isExternal()) {
                    ivExternal.setVisibility(View.VISIBLE);
                    UIHelper.setColorFilter(ivExternal);
                } else {
                    ivExternal.setVisibility(View.GONE);
                }
            }

            ImageView ivHasUpdates = (ImageView) row.findViewById(R.id.novel_has_updates);
            if (ivHasUpdates != null) {
                if (page.getUpdateCount() > 0) {
                    ivHasUpdates.setVisibility(View.VISIBLE);
                    UIHelper.setColorFilter(ivHasUpdates);
                } else {
                    ivHasUpdates.setVisibility(View.GONE);
                }
            }
        }

        holder.txtLastUpdate = (TextView) row.findViewById(R.id.novel_last_update);
        if (holder.txtLastUpdate != null) {
            holder.txtLastUpdate.setText(context.getResources().getString(R.string.last_update) + ": " + Util.formatDateForDisplay(context, page.getLastUpdate()));
        }

        holder.txtLastCheck = (TextView) row.findViewById(R.id.novel_last_check);
        if (holder.txtLastCheck != null) {
            holder.txtLastCheck.setText(context.getResources().getString(R.string.last_check) + ": " + Util.formatDateForDisplay(context, page.getLastCheck()));
        }

        holder.chkIsWatched = (CheckBox) row.findViewById(R.id.novel_is_watched);
        if (holder.chkIsWatched != null) {
            // Log.d(TAG, page.getId() + " " + page.getTitle() + " isWatched: " + page.isWatched());
            holder.chkIsWatched.setChecked(page.isWatched());
            holder.chkIsWatched.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        Toast.makeText(context, "Added to watch list: " + page.getTitle(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Removed from watch list: " + page.getTitle(), Toast.LENGTH_SHORT).show();
                    }
                    // update the db!
                    page.setWatched(isChecked);
                    NovelsDao.getInstance().updatePageModel(page);
                }
            });
        }

        row.setTag(holder);
        return row;
    }

    public void filterData() {
        this.clear();
        data.clear();
        for (PageModel item : originalData) {
            if (!item.isHighlighted()) {
                if (!UIHelper.getShowRedlink(getContext()) && item.isRedlink())
                    continue;
                if (!UIHelper.getShowMissing(getContext()) && item.isMissing())
                    continue;
                if (!UIHelper.getShowExternal(getContext()) && item.isExternal())
                    continue;
            }
            data.add(item);
        }
        super.notifyDataSetChanged();
        Log.d(TAG, "Filtered result : " + data.size());
    }

    // somehow if enabled, will trigger the db 2x (first load and after load)
    @Override
    public void notifyDataSetChanged() {
        synchronized (this) {
            // refresh the data
            Log.d(TAG, "Refreshing data: " + data.size() + " items");
            if (!UIHelper.getQuickLoad(context)) {
                for (int i = 0; i < data.size(); ++i) {
                    try {
                        PageModel temp = NovelsDao.getInstance().getPageModel(data.get(i), null);
                        temp.setUpdateCount(data.get(i).getUpdateCount());
                        data.set(i, temp);
                    } catch (Exception e) {
                        Log.e(TAG, "Error when refreshing PageModel: " + data.get(i).getPage(), e);
                    }
                }
            }
            super.notifyDataSetChanged();
        }
    }

    static class PageModelHolder {
        TextView txtNovel;
        TextView txtLastUpdate;
        TextView txtLastCheck;
        CheckBox chkIsWatched;
    }

    public void setResourceId(int id) {
        this.layoutResourceId = id;
    }
}
