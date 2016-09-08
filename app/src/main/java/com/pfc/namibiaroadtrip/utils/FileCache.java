package com.pfc.namibiaroadtrip.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.text.TextUtils;

import com.pfc.namibiaroadtrip.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;

import static android.os.Environment.MEDIA_MOUNTED;

/**
 * 文件存储类
 * 
 * @author user
 * 
 */
public class FileCache {

	public static final String CACHE_FOLDER = "filecache" + File.separator;
	public static final String CACHE_FOLDER_CACHES = "caches" + File.separator;
	public static final String CACHE_FOLDER_NAME = File.separator + "namibia"
			+ File.separator;
	
	
	
	public static final int CACHE_TYPE_SDCARD = 1;
	public static final int CACHE_TYPE_CACHEFILE = 2;
	public static final int CACHE_TYPE_ALL = 3;

	/**
	 * 存储文件
	 * 
	 * @param context
	 * @param object
	 *            存储对象
	 *
	 *            子目录
	 * @param filename
	 *            文件名
	 */
	public static void save(Context context, Object object, String filename) {
		String filePath = "";
		filePath = getCacheFolder(context, CACHE_FOLDER_CACHES,
				CACHE_TYPE_CACHEFILE);
		filePath += CodeUtil.md5(filename);
		save(object, filePath);
	}

	/**
	 * 加载本地文件
	 * 	
	 * @param context
	 * @param filename
	 * @return
	 */
	public static Object load(Context context, String filename) {
		String filePath = "";	
		// 文件信息缓存 或者user对象缓存（存放在缓存系统缓存文件夹中）
		filePath = getCacheFolder(context, CACHE_FOLDER_CACHES,
				CACHE_TYPE_CACHEFILE);
		filePath += CodeUtil.md5(filename);
		return load(filePath);
	}

	/**
	 * 得到文件存储目录
	 * 
	 * @param context
	 * @param subFolder
	 * @param cache_type
	 * @return
	 */
	public static String getCacheFolder(Context context, String subFolder,
			int cache_type) {
		String cacheDir;
		File file;
		// 要在缓存文件夹中,不能存放在sd卡中
		if (cache_type == CACHE_TYPE_SDCARD) {
			if (Environment.getExternalStorageState().equals(MEDIA_MOUNTED)) {
				file = Environment.getExternalStorageDirectory();
				cacheDir = file.getPath() + CACHE_FOLDER_NAME;
			} else
				cacheDir = context.getCacheDir().getAbsolutePath()
						+ CACHE_FOLDER_NAME;
		} else {
			cacheDir = context.getCacheDir().getAbsolutePath()
					+ CACHE_FOLDER_NAME;
		}

		if (!TextUtils.isEmpty(subFolder)) {
			cacheDir += subFolder + File.separator;
		}
		File cacheDirFile = new File(cacheDir);
		if (!cacheDirFile.exists()) {
			synchronized (cacheDirFile) {
				if (!cacheDirFile.exists())
					cacheDirFile.mkdirs();
			}
		}
		return cacheDir;
	}

	/**
	 * 清除数据缓存
	 * 清除文件的位置,SD卡，缓存文件，所有缓存
	 * @param
	 *
	 */
	public static void clearCacheFile(Context context, String fileName)
			throws IOException {
		clearCacheFile(context, CACHE_TYPE_CACHEFILE, fileName);
	}

	/**
	 * 清除数据缓存
	 * 
	 * @param cache_type
	 *            清除文件的位置,SD卡，缓存文件，所有缓存
	 * @param filename
	 *            如果要删除特定的文件，需要传入文件名称
	 */
	public static void clearCacheFile(Context context, int cache_type,
			String filename) throws IOException {
		String filePath = "";
		// 删除SD卡中的东西
		if (CACHE_TYPE_SDCARD == cache_type) {
			filePath = getCacheFolder(context, "", CACHE_TYPE_SDCARD);
		}
		// 文件信息缓存 （存放在缓存系统缓存文件夹中）
		else if (CACHE_TYPE_CACHEFILE == cache_type) {
			filePath = getCacheFolder(context, CACHE_FOLDER_CACHES,
					CACHE_TYPE_CACHEFILE);
		}
		if (!TextUtils.isEmpty(filename))
			filePath += filename;
		if (!TextUtils.isEmpty(filePath))
			deleteCacheDir(new File(filePath));
		// 删除全部缓存
		if (CACHE_TYPE_ALL == cache_type) {
			filePath = getCacheFolder(context, "", CACHE_TYPE_SDCARD);
			deleteCacheDir(new File(filePath));
			filePath = getCacheFolder(context, CACHE_FOLDER,
					CACHE_TYPE_CACHEFILE);
			deleteCacheDir(new File(filePath));
			filePath = getCacheFolder(context, CACHE_FOLDER_CACHES,
					CACHE_TYPE_CACHEFILE);
			deleteCacheDir(new File(filePath));
		}
	}

	/**
	 * 递归删除文件
	 */
	public static void deleteCacheDir(File dir) throws IOException {
		if (dir != null) {
			if (dir.isFile()) {
				dir.delete();
			} else {
				File[] entries = dir.listFiles();
				if (entries != null) {
					for (File entry : entries) {
						if (entry.isDirectory()) {
							deleteCacheDir(entry);
						} else {
							entry.delete();
						}
					}
				}
				dir.delete();
			}
		}
	}

	/**
	 * 通过对象流从缓存文件中读取数据
	 * 
	 * @param filename
	 *            文件名
	 */
	public static Object load(String filename) {
		ObjectInputStream ois = null;
		Object object = null;
		File file = new File(filename);
		try {
			if (file.exists()) {
				ois = new ObjectInputStream(new FileInputStream(file));
				object = ois.readObject();
			}
		} catch (Exception e) {
		} finally {
			if (ois != null)
				try {
					ois.close();
				} catch (IOException e) {
				}
		}
		return object;
	}

	/**
	 * 通过对象流存储到缓存文件
	 * 
	 * @param object
	 *            对象
	 * @param filename
	 *            文件名
	 */
	public static void save(Object object, String filename) {
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(new FileOutputStream(
					new File(filename)));
			oos.writeObject(object);
			oos.flush();
		} catch (Exception e) {
		} finally {
			if (oos != null)
				try {
					oos.close();
				} catch (IOException e) {
				}
		}
	}
	public static void saveImage(Context context){
		Bitmap bitmap=BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_ar_list_hot);
		try {
			bitmap.compress(Bitmap.CompressFormat.PNG,100,new FileOutputStream(context.getCacheDir().getAbsolutePath()+"/chinahr.png"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	public static String saveMyBitmap(Bitmap mBitmap) {
		File tempFile;// 创建一个以当前时间为名称的文件
		String status = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(status)) {
			File dataDir = Environment.getExternalStorageDirectory();
			File myDir = new File(dataDir,"/chinahrshareQQphoto");
			myDir.mkdirs();
			String mDirectoryName = dataDir.toString() + "/chinahrshareQQphoto";
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-hhmmss");
			tempFile = new File(mDirectoryName,
					sdf.format(new java.util.Date()) + ".jpg");
			if (tempFile.isFile()) {
				tempFile.delete();
			}
			try {
				FileOutputStream fOut = new FileOutputStream(tempFile);
				mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
				fOut.flush();
				fOut.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			String imageUrl = tempFile.toString();
			return  imageUrl;
		} else {
//			ToastUtil.showShortToast(MyApp.mContext, "请插入存储卡");
			return "";
		}
	}
	/**
	 * Bitmap → byte[]
	 */
	public static byte[] Bitmap2Bytes(Bitmap bm) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
		return baos.toByteArray();
	}
}
