package com.fileManager.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.utils.FileUtils;
import com.blankj.utilcode.utils.TimeUtils;
import com.fileManager.R;
import com.fileManager.util.ACache;
import com.fileManager.util.FileUtil;
import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by 齐泽威 on 2016/12/7.
 */

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MyViewHolder> {
    private List<File> mDatas;
    private Context mContext;
    private MusicAdapter.OnItemClickLitener mOnItemClickLitener;
    private ACache mCache;
    private Gson mGson;


    public MusicAdapter(Context context, List<File> Data) {
        this.mDatas = Data;
        this.mContext = context;
        this.mGson = new Gson();
        try {
            mCache = ACache.get(mContext);
        }catch (Exception e){
            //子线程未销毁可能时执行
        }

    }

    public void setOnItemClickLitener(MusicAdapter.OnItemClickLitener mOnItemClickLitener) {
        this.mOnItemClickLitener = mOnItemClickLitener;
    }

    @Override
    public MusicAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        MusicAdapter.MyViewHolder holder = new MusicAdapter.MyViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_music, parent,
                false));
        return holder;
    }

    @Override
    public void onBindViewHolder(final MusicAdapter.MyViewHolder holder, final int position) {
        holder.tv.setText(mDatas.get(position).getName());

        // 如果设置了回调，则设置点击事件
        if (mOnItemClickLitener != null) {
            holder.linear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getLayoutPosition();
                    mOnItemClickLitener.onItemClick(holder.linear, pos);

                    String path = mDatas.get(pos).getPath();
                    Intent intent = FileUtil.openFile(path);
                    mContext.startActivity(intent);

                }
            });

            holder.linear.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    final String items[] = {"重命名文件", "文件详情","分享"};
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);  //先得到构造器
                    builder.setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //dialog.dismiss();  
                            if (which == 0) {
                                ReName(position);
                            } else if (which == 1)
                                ShowDetial(position);
                            else if (which ==2) {
                                Intent intent = new Intent(Intent.ACTION_SEND);
                                intent.setType("audio/*");
                                Uri uri = Uri.fromFile(mDatas.get(position)); intent.putExtra(Intent.EXTRA_STREAM, uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                mContext.startActivity(Intent.createChooser(intent,"分享到"));
                            }
                        }
                    });
                    builder.create().show();
                    return false;

                }
            });

            holder.item_music_delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getLayoutPosition();
                    mOnItemClickLitener.onItemClick(holder.tv, pos);
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setTitle("确认删除");
                    builder.setMessage("是否确认删除该文件");
                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            removeData(holder.getAdapterPosition());
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();

                    synchronized (this) {
                        this.notify();
                    }
                }
            });
            
            
        }
    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }

    private void removeData(int position) {
        String path = mDatas.get(position).getAbsolutePath();
        FileUtils.deleteFile(path);

        for (int i = 0; i < mDatas.size(); i++) {
            String s = String.valueOf(i);
            mCache.remove(s+"music");
        }
        
        mDatas.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position,position+1);
        //reset all catch
        ArrayList<String> strings = new ArrayList<>();
        for (int i = 0; i < mDatas.size(); i++) {
            String s = mGson.toJson(mDatas.get(i));
            strings.add(s);
        }
        for (int i = 0; i < strings.size(); i++) {
            String s = String.valueOf(i);
            mCache.put(s+"music", strings.get(i), ACache.TIME_DAY);
        }
    }

    public interface OnItemClickLitener {
        void onItemClick(View view, int position);

        void onItemLongClick(View view, int position);
    }

    private void ShowDetial(int position) {
        File file = mDatas.get(position);
        String size = FileUtils.getFileSize(file);
        String name = file.getName();
        String path = file.getAbsolutePath();
        String time = TimeUtils.milliseconds2String(file.lastModified());

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("文件属性")
                .setCancelable(false)
                .setNegativeButton("确定", null)
                .setMessage("\n" + "文件名：" + name + "\n\n" + "文件大小：" + size + "\n\n" + "文件路径：" +
                        path + "\n\n" + "时间：" + time )
                .show();

    }

    private void ReName(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        final EditText userId = new EditText(mContext);
        builder.setTitle("请输入新命名：")
                .setCancelable(false)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String newName = userId.getText().toString().trim();
                        if (newName.equals("")) {
                            Toast.makeText(mContext, "输入不能为空", Toast.LENGTH_SHORT).show();
                        } else {
                            //重命名File
                            FileUtils.rename(mDatas.get(position), newName + ".mp3");

                            //更新显示数据
                            String path = mDatas.get(position).getParent();
                            File file = new File(path + "/" + newName + ".mp3");
                            mDatas.remove(position);
                            mDatas.add(position, file);
                            notifyDataSetChanged();
                            Toast.makeText(mContext, "重命名文件成功", Toast.LENGTH_SHORT).show();

                            //更新缓存
                            String s = String.valueOf(position);
                            String name = "{\"path\":\"" + file.getAbsolutePath() + "\"}";
                            mCache.put(s + "music", name);

                        }
                    }
                })
                .setView(userId, 150, 20, 70, 20)
                .show();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tv, item_music_delete;
        LinearLayout linear;

        public MyViewHolder(View view) {
            super(view);
            tv = (TextView) view.findViewById(R.id.item_music_filename);
            linear = (LinearLayout)view.findViewById(R.id.music_linear); 
            item_music_delete = (TextView) view.findViewById(R.id.item_music_delete);
        }
    }

}

