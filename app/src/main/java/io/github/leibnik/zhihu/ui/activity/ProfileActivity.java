package io.github.leibnik.zhihu.ui.activity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import butterknife.Bind;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.SaveListener;
import cn.bmob.v3.listener.UpdateListener;
import cn.bmob.v3.listener.UploadFileListener;
import io.github.leibnik.zhihu.R;
import io.github.leibnik.zhihu.ZhihuApplication;
import io.github.leibnik.zhihu.adapter.StarSummaryAdapter;
import io.github.leibnik.zhihu.base.BaseActivity;
import io.github.leibnik.zhihu.constant.Constant;
import io.github.leibnik.zhihu.entity.Summary;
import io.github.leibnik.zhihu.entity.User;
import io.github.leibnik.zhihu.manager.LoginManager;
import io.github.leibnik.zhihu.manager.UpdateInfoManager;
import io.github.leibnik.zhihu.task.TaskBlurBackground;
import io.github.leibnik.zhihu.ui.dialog.EditPasswordDialog;
import io.github.leibnik.zhihu.ui.dialog.EditUsernameDialog;
import io.github.leibnik.zhihu.util.BitmapUtil;
import io.github.leibnik.zhihu.util.PreferenceUtil;

/**
 * Created by Droidroid on 2016/3/30.
 */
public class ProfileActivity extends BaseActivity {
    @Bind(R.id.toolbar)
    Toolbar mToolbar;
    @Bind(R.id.recyclerview)
    RecyclerView mRecyclerView;
    @Bind(R.id.appbarlayout)
    AppBarLayout mAppbarLayout;
    @Bind(R.id.background)
    ImageView backgroundIv;
    @Bind(R.id.collapsinglayout)
    CollapsingToolbarLayout mCollapsingToolbarLayout;

    @Bind(R.id.avatar)
    ImageView avatarIv;
    @Bind(R.id.name)
    TextView nameTv;
    @Bind(R.id.name_editor)
    ImageView nameEditorIv;

    private int pagesize = 10;
    private int pageindex = 0;
    private boolean isLoading = false;
    private boolean isColorTheme = true;
    private boolean isAutoLogin = false;
    private StarSummaryAdapter mAdapter;

    @Override
    protected void initVariables() {
        super.initVariables();
        ZhihuApplication.user = BmobUser.getCurrentUser(getApplicationContext(), User.class);
        isColorTheme = PreferenceUtil.getPrefBoolean(getApplicationContext(), "isColorTheme", true);
        isAutoLogin = PreferenceUtil.getPrefBoolean(getApplicationContext(), "isAutoLogin", false);
    }

    @Override
    protected int setLayout() {
        return R.layout.activity_profile;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        initTheme();

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        mAppbarLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        mCollapsingToolbarLayout.setTitle("我的收藏");
        mCollapsingToolbarLayout.setContentScrimColor(getResources().getColor(R.color.blue));

        nameTv.setText(ZhihuApplication.user.getUsername());
        nameEditorIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), EditUsernameDialog.class);
                startActivityForResult(intent, Constant.EDIT_NAME);
            }
        });
        avatarIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, Constant.ALBUTM);
            }
        });

        final LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (pageindex != 0 && !isLoading && layoutManager.findLastVisibleItemPosition() == mAdapter.getItemCount() - 1) {
                    isLoading = true;
                    loadMoreData();
                }
            }
        });
    }

    private void initTheme() {
        if (isColorTheme) {
            backgroundIv.setBackgroundColor(getResources().getColor(R.color.blue));
            Glide.with(getApplicationContext()).load(ZhihuApplication.user.getAvatar())
                    .error(R.mipmap.profile).into(avatarIv);
        } else {
            if (TextUtils.isEmpty(ZhihuApplication.user.getAvatar())) {
                new TaskBlurBackground(getApplicationContext(), R.mipmap.profile) {
                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
                        super.onPostExecute(bitmap);
                        backgroundIv.setImageBitmap(bitmap);
                    }
                }.execute();
            } else {
                Glide.with(getApplicationContext()).load(ZhihuApplication.user.getAvatar()).listener(new RequestListener<String, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        new TaskBlurBackground(BitmapUtil.drawableToBitmap(resource)) {
                            @Override
                            protected void onPostExecute(Bitmap bitmap) {
                                super.onPostExecute(bitmap);
                                backgroundIv.setImageBitmap(bitmap);
                            }
                        }.execute();
                        return false;
                    }
                }).error(R.mipmap.profile).into(avatarIv);
            }
        }
    }

    private void loadMoreData() {
        BmobQuery<Summary> query = new BmobQuery<>();
        query.setLimit(pagesize);
        query.setSkip(pageindex * pagesize);
        query.order("-createdAt");
        query.addWhereEqualTo("user", ZhihuApplication.user.getObjectId());
        query.findObjects(getApplicationContext(), new FindListener<Summary>() {
            @Override
            public void onSuccess(List<Summary> list) {
                if (list != null && list.size() > 0) {
                    // 数据去重
                    LinkedHashSet<Summary> set = new LinkedHashSet<>(list);
                    List<Summary> summaryList = new ArrayList<>(set);
                    mAdapter.addData(summaryList);
                    pageindex++;
                    isLoading = false;
                    if (list.size() < pagesize) {
                        mAdapter.setIsFooterGone(true);
                    }
                }

            }

            @Override
            public void onError(int i, String s) {

            }
        });
    }

    @Override
    protected void loadData() {
        super.loadData();
        if (ZhihuApplication.user == null) {
            return;
        }
        mAdapter = new StarSummaryAdapter(ProfileActivity.this);
        mAdapter.setOnItemClickListener(new StarSummaryAdapter.OnItemClickListener() {
            @Override
            public void OnItemClick(View v, Summary data) {
                Intent intent = null;
                int[] location = new int[2];
                v.getLocationOnScreen(location);
                location[0] += v.getWidth() / 2;
                if (data.getType() == Constant.TYPE_DAY_DETAIL) {
                    intent = new Intent(getApplicationContext(), DayNewsDetailActivity.class);
                } else if (data.getType() == Constant.TYPE_THEME_DETAIL) {
                    intent = new Intent(getApplicationContext(), ThemeNewsDetailActivity.class);
                }
                if (intent != null) {
                    intent.putExtra("summary", data);
                    intent.putExtra("location", location);
                    startActivity(intent);
                }
            }
        });
        mRecyclerView.setAdapter(mAdapter);
        BmobQuery<Summary> query = new BmobQuery<>();
        query.setLimit(pagesize);
        query.setSkip(pageindex * pagesize);
        query.order("-createdAt");
        query.addWhereEqualTo("user", ZhihuApplication.user.getObjectId());
        query.findObjects(getApplicationContext(), new FindListener<Summary>() {
            @Override
            public void onSuccess(List<Summary> list) {
                if (list != null && list.size() > 0) {
                    // 数据去重
                    LinkedHashSet<Summary> set = new LinkedHashSet<>(list);
                    List<Summary> summaryList = new ArrayList<>(set);
                    mAdapter.setData(summaryList);
                    pageindex++;
                    if (list.size() < pagesize) {
                        mAdapter.setIsFooterGone(true);
                    }
                } else {
                    mAdapter.setIsFooterGone(true);
                    Snackbar.make((View) mRecyclerView.getParent(), "暂无收藏", Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(int i, String s) {
                mAdapter.setIsFooterGone(true);
                Snackbar.make((View) mRecyclerView.getParent(), s, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_profile, menu);
        menu.getItem(1).setTitle(isAutoLogin ? "关闭自动登陆" : "开启自动登陆");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.logout) {
            BmobUser.logOut(getApplicationContext());
            LoginManager.logout();
            ZhihuApplication.user = null;
            finish();
            overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
        }
        if (id == R.id.auto_login) {
            isAutoLogin = !isAutoLogin;
            PreferenceUtil.setPrefBoolean(getApplicationContext(), "isAutoLogin", isAutoLogin);
            Snackbar.make((View) mAppbarLayout.getParent(), isAutoLogin ? "已开启自动登陆" : "已关闭自动登陆", Snackbar.LENGTH_SHORT).show();
            item.setTitle(isAutoLogin ? "关闭自动登陆" : "开启自动登陆");
        }
        if (id == R.id.reset_password) {
            Intent intent = new Intent(getApplicationContext(), EditPasswordDialog.class);
            startActivityForResult(intent, Constant.RESET_PASSWORD);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {

            if (requestCode == Constant.ALBUTM && data != null) {
                Uri selectedImage = data.getData();
                String[] filePathColumns = {MediaStore.Images.Media.DATA};

                Cursor c = this.getContentResolver().query(selectedImage, filePathColumns, null, null, null);
                c.moveToFirst();
                int columnIndex = c.getColumnIndex(filePathColumns[0]);
                String picturePath = c.getString(columnIndex);
                c.close();

                final File file = new File(picturePath);
                bmobUploadAvatar(file);
            }
            if (requestCode == Constant.EDIT_NAME) {
                nameTv.setText(ZhihuApplication.user.getUsername());
                UpdateInfoManager.updateInfo();
                Snackbar.make((View) mRecyclerView.getParent(), "成功更新用户名", Snackbar.LENGTH_SHORT).show();
            }
            if (requestCode == Constant.RESET_PASSWORD) {
                Snackbar.make((View) mRecyclerView.getParent(), "重置密码成功", Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private void bmobUploadAvatar(final File file) {
        final BmobFile bmobFile = new BmobFile(file);
        Snackbar.make((View) mRecyclerView.getParent(), "头像上传中...", Snackbar.LENGTH_INDEFINITE).show();
        bmobFile.uploadblock(this, new UploadFileListener() {

            @Override
            public void onSuccess() {
//                ZhihuApplication.user.setAvatar(bmobFile.getFileUrl(getApplicationContext()));
                User user = new User();
                user.setAvatar(bmobFile.getFileUrl(getApplicationContext()));
                user.update(getApplicationContext(), ZhihuApplication.user.getObjectId(), new UpdateListener() {
                    @Override
                    public void onSuccess() {
                        ZhihuApplication.user.setAvatar(bmobFile.getFileUrl(getApplicationContext()));
                        UpdateInfoManager.updateInfo();
                        Glide.with(getApplicationContext()).load(ZhihuApplication.user.getAvatar()).listener(new RequestListener<String, GlideDrawable>() {
                            @Override
                            public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                                new TaskBlurBackground(BitmapUtil.drawableToBitmap(resource)) {
                                    @Override
                                    protected void onPostExecute(Bitmap bitmap) {
                                        super.onPostExecute(bitmap);
                                        backgroundIv.setImageBitmap(bitmap);
                                    }
                                }.execute();
                                return false;
                            }
                        }).error(R.mipmap.profile).into(avatarIv);
                        Snackbar.make((View) mRecyclerView.getParent(), "头像上传成功", Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int i, String s) {
                        Snackbar.make((View) mRecyclerView.getParent(), s, Snackbar.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onProgress(Integer value) {
                // TODO Auto-generated method stub
                // 返回的上传进度（百分比）
            }

            @Override
            public void onFailure(int code, String msg) {
                // TODO Auto-generated method stub
                Snackbar.make((View) mRecyclerView.getParent(), msg, Snackbar.LENGTH_SHORT).show();
            }
        });
    }
}
