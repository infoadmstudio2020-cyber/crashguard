package com.roni.crashguard.demo.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.roni.library.crashguard.CrashReport;

import java.util.ArrayList;
import java.util.List;

/**
 * সংরক্ষিত ক্র্যাশ রিপোর্টের তালিকা দেখানোর সাধারণ অ্যাডাপ্টার।
 */
public class CrashReportAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final List<CrashReport> reports = new ArrayList<>();

    public CrashReportAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
    }

    public void submit(List<CrashReport> newReports) {
        reports.clear();
        if (newReports != null) {
            reports.addAll(newReports);
        }
        notifyDataSetChanged();
    }

    public CrashReport getItemAt(int position) {
        return reports.get(position);
    }

    @Override
    public int getCount() {
        return reports.size();
    }

    @Override
    public CrashReport getItem(int position) {
        return reports.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_crash_report, parent, false);
            holder = new ViewHolder();
            holder.badge = convertView.findViewById(R.id.tv_item_badge);
            holder.title = convertView.findViewById(R.id.tv_item_title);
            holder.message = convertView.findViewById(R.id.tv_item_message);
            holder.meta = convertView.findViewById(R.id.tv_item_meta);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        CrashReport report = reports.get(position);

        holder.badge.setText(report.isHandledReport()
            ? R.string.demo_badge_handled
            : R.string.demo_badge_uncaught);
        holder.badge.setBackgroundResource(report.isHandledReport()
            ? R.drawable.bg_badge_handled
            : R.drawable.bg_badge_uncaught);

        holder.title.setText(report.getExceptionClass());
        holder.message.setText(report.getExceptionMessage());
        holder.meta.setText(report.getFormattedDate()
            + "  •  " + report.getThreadName()
            + (report.isMainThread() ? " (main)" : ""));

        return convertView;
    }

    private static class ViewHolder {
        TextView badge;
        TextView title;
        TextView message;
        TextView meta;
    }
}
