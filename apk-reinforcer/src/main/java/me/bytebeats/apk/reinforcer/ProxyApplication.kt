package me.bytebeats.apk.reinforcer

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * @Author bytebeats
 * @Email <happychinapc@gmail.com>
 * @Github https://github.com/bytebeats
 * @Created at 2022/3/28 18:04
 * @Version 1.0
 * @Description TO-DO
 */

class ProxyApplication : Application() {
    //定义好的加密后的文件的存放路径
    private var appName: String? = null
    private var appVersion: String? = null

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        //获取用户填入的metaData
        appName = getMetaString("app_name")
        appVersion = getMetaString("app_version")
        decryptDexs()
    }

    override fun onCreate() {
        super.onCreate()
        try {
            bindRealApplication()
        } catch (ignore: Exception) {
            ignore.printStackTrace()
        }
    }

    override fun createPackageContext(packageName: String?, flags: Int): Context {
        return if (appName.isNullOrEmpty()) {
            super.createPackageContext(packageName, flags)
        } else {
            try {
                bindRealApplication()
            } catch (ignore: Exception) {
                ignore.printStackTrace()
            }
            delegate
        }
    }

    private fun decryptDexs() {
        //得到当前apk文件
        val apkFile = File(applicationInfo.sourceDir)
        //把apk解压, 这个目录中的内容需要root权限才能使用
        // TODO: 2022/3/28 找到合适的解压目录
        val unzipDir = File(this.filesDir, "$appName-$appVersion")
        val appDir = File(unzipDir, "app")
        val dexDir = File(appDir, "dexDir")
        //得到我们需要加载的dex文件
        val dexFiles = mutableListOf<File>()
        //进行解密 （最好做md5文件校验）
        if (!dexDir.exists() || dexDir.list()?.size == 0) {
            //把apk解压到appDir
            decompress(apkFile.absolutePath, appDir.absolutePath)
            //获取目录下所有的文件
            val files = appDir.listFiles()
            files.forEach { file ->
                val name = file.name
                if (name.endsWith(".dex") && name != "classes.dex") {
                    try {
                        //读取文件内容
                        val bytes = getByteFromFile(file)
                        //解密
                        val decrypted = decrypt(bytes)
                        //写到指定的目录
                        val fos = FileOutputStream(file)
                        fos.write(decrypted)
                        fos.flush()
                        fos.close()

                        dexFiles.add(file)
                    } catch (ignore: Exception) {
                        ignore.printStackTrace()
                    }
                }
            }
        } else {
            dexDir.listFiles().forEach { file -> dexFiles.add(file) }
        }

        try {
            loadDexs(dexFiles, unzipDir)
        } catch (ignore: Exception) {
            ignore.printStackTrace()
        }
    }

    private fun loadDexs(dexFiles: List<File>, unzipDir: File) {
        //1、获取pathList
        val pathListField = classLoader.javaClass.getField("pathList")
        val pathListObj = pathListField.get(classLoader)

        //2、获取数组dexElements
        val dexElementsField = pathListObj.javaClass.getField("dexElements")
        val dexElementsObj = dexElementsField.get(pathListObj) as Array<*>

        //3、反射到初始化makePathElements的方法
        val makeDexElementsMethod =
            pathListObj.javaClass.getMethod("makeDexElements", List::class.java, File::class.java, List::class.java)
        val suppressedException = mutableListOf<IOException>()
        val addElements = makeDexElementsMethod.invoke(pathListObj, dexFiles, unzipDir, suppressedException) as Array<*>

        val newElements = java.lang.reflect.Array.newInstance(
            dexElementsObj.javaClass.componentType,
            dexElementsObj.size + addElements.size
        )
        System.arraycopy(dexElementsObj, 0, newElements, 0, dexElementsObj.size)
        System.arraycopy(addElements, 0, newElements, 0, addElements.size)

        //替换classloader中的element数组
        dexElementsField.set(pathListObj, newElements)
    }

    private fun getMetaString(name: String): String? = try {
        val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        val metadata = appInfo.metaData
        metadata?.getString(name)
    } catch (ignore: Exception) {
        null
    }

    /**
     * 让代码走入if的第三段中
     * @return
     */
    override fun getPackageName(): String {
        if (!appName.isNullOrEmpty()) {
            return ""
        }
        return super.getPackageName()
    }

    private var isRealAppBond = false
    private lateinit var delegate: Application

    //下面主要是通过反射系统源码的内容，然后进行处理，把我们的内容加进去处理
    @Throws(Exception::class)
    private fun bindRealApplication() {
        if (isRealAppBond) return
        if (appName.isNullOrEmpty()) return

        // 得到attachBaseContext(context) 传入的上下文 ContextImpl
        val baseCtx = baseContext
        val delegateAppClass = Class.forName(appName!!)
        delegate = delegateAppClass.newInstance() as Application

        //得到attach()方法
        val attachMethod = Application::class.java.getDeclaredMethod("attach", Context::class.java)
        attachMethod.isAccessible = true
        attachMethod.invoke(delegate, baseCtx)
        //获取ContextImpl ----> ,mOuterContext(app);  通过Application的attachBaseContext回调参数获取
        val ctxImplClass = Class.forName("android.app.ContextImpl")
        //获取mOuterContext属性
        val outerCtx = ctxImplClass.getDeclaredField("mOuterContext")
        outerCtx.isAccessible = true
        outerCtx.set(baseCtx, delegate)

        //ActivityThread  ----> mAllApplication(ArrayList)  ContextImpl的mMainThread属性
        val mainThreadF = ctxImplClass.getDeclaredField("mMainThread")
        mainThreadF.isAccessible = true
        val mainThreadObj = mainThreadF.get(baseCtx)

        //ActivityThread  ----->  mInitialApplication       ContextImpl的mMainThread属性
        val aThreadClass = Class.forName("android.app.ActivityThread")
        val initAppF = aThreadClass.getDeclaredField("mInitApplication")
        initAppF.isAccessible = true
        initAppF.set(mainThreadObj, delegate)

        //ActivityThread ------>  mAllApplications(ArrayList)   ContextImpl的mMainThread属性
        val allApplicationsF = aThreadClass.getDeclaredField("mAllApplications")
        allApplicationsF.isAccessible = true
        val allApplicationObj = allApplicationsF.get(mainThreadObj) as ArrayList<Application>
        allApplicationObj.remove(this)
        allApplicationObj.add(delegate)

        //LoadedApk ----->  mApplicaion             ContextImpl的mPackageInfo属性
        val pkgInfoF = ctxImplClass.getDeclaredField("mPackageInfo")
        pkgInfoF.isAccessible = true
        val pkgInfoObj = pkgInfoF.get(baseCtx)

        val loadedApkClass = Class.forName("android.app.LoadedApk")
        val applicationF = loadedApkClass.getDeclaredField("mApplication")
        applicationF.isAccessible = true
        applicationF.set(pkgInfoF, delegate)

        //修改ApplicationInfo  className  LoadedApk
        val appInfoF = loadedApkClass.getDeclaredField("mApplicationInfo")
        appInfoF.isAccessible = true
        val appInfoObj = appInfoF.get(pkgInfoObj) as ApplicationInfo
        appInfoObj.className = appName

        delegate.onCreate()
        isRealAppBond = true
    }
}