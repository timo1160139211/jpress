/**
 * Copyright (c) 2016-2019, Michael Yang 杨福海 (fuhai999@gmail.com).
 * <p>
 * Licensed under the GNU Lesser General Public License (LGPL) ,Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jpress.core.addon;


import com.jfinal.aop.Aop;
import com.jfinal.aop.Interceptor;
import com.jfinal.core.Controller;
import com.jfinal.handler.Handler;
import com.jfinal.log.Log;
import io.jboot.aop.annotation.Bean;
import io.jboot.aop.annotation.BeanExclude;
import io.jboot.components.event.JbootEventListener;
import io.jboot.components.mq.JbootmqMessageListener;
import io.jboot.db.model.JbootModel;
import io.jboot.utils.ArrayUtil;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class AddonClassLoader extends URLClassLoader {

    private static final Log LOG = Log.getLog(AddonClassLoader.class);

    private AddonInfo addonInfo;
    private List<String> classNameList;

    public AddonClassLoader(AddonInfo addonInfo) throws IOException {
        super(new URL[] {}, Thread.currentThread().getContextClassLoader());
        this.addURL(addonInfo.buildJarFile().toURI().toURL());
        this.addonInfo = addonInfo;
        this.classNameList = new ArrayList<>();
        this.initClassNameList();
    }

    public List<String> getClassNameList() {
        return classNameList;
    }

    private void initClassNameList() throws IOException {
        Enumeration<JarEntry> entries = new JarFile(addonInfo.buildJarFile()).entries();
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            String entryName = jarEntry.getName();
            if (!jarEntry.isDirectory() && entryName.endsWith(".class")) {
                String className = entryName.replace("/", ".").substring(0, entryName.length() - 6);
                classNameList.add(className);
            }
        }
    }


    public void load() {
        for (String className : classNameList) {
            try {

                Class loadedClass = loadClass(className);

                Bean bean = (Bean) loadedClass.getDeclaredAnnotation(Bean.class);
                if (bean != null) {
                    initBeanMapping(loadedClass);
                }

                // controllers
                if (Controller.class.isAssignableFrom(loadedClass)) {
                    addonInfo.addController(loadedClass);
                }
                // interceptors
                else if (Interceptor.class.isAssignableFrom(loadedClass)) {
                    addonInfo.addInterceptor(loadedClass);
                }
                // handlers
                else if (Handler.class.isAssignableFrom(loadedClass)) {
                    addonInfo.addHandler(loadedClass);
                }
                // models
                else if (JbootModel.class.isAssignableFrom(loadedClass)) {
                    addonInfo.addModel(loadedClass);
                }
                // addonClass
                else if (Addon.class.isAssignableFrom(loadedClass)) {
                    addonInfo.setAddonClass(loadedClass);
                }

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }


    private static Class[] default_excludes = new Class[]{JbootEventListener.class, JbootmqMessageListener.class, Serializable.class};

    /**
     * 初始化 @Bean 注解的映射关系
     */
    private void initBeanMapping(Class implClass) {

        Class<?>[] interfaceClasses = implClass.getInterfaces();

        if (interfaceClasses == null || interfaceClasses.length == 0) {
            return;
        }

        Class[] excludes = buildExcludeClasses(implClass);

        for (Class interfaceClass : interfaceClasses) {
            if (inExcludes(interfaceClass, excludes) == false) {
                Aop.getAopFactory().addMapping(interfaceClass, implClass);
            }
        }
    }

    private Class[] buildExcludeClasses(Class implClass) {
        BeanExclude beanExclude = (BeanExclude) implClass.getAnnotation(BeanExclude.class);

        //对某些系统的类 进行排除，例如：Serializable 等
        return beanExclude == null
                ? default_excludes
                : ArrayUtil.concat(default_excludes, beanExclude.value());
    }

    private boolean inExcludes(Class interfaceClass, Class[] excludes) {
        for (Class ex : excludes) {
            if (ex.isAssignableFrom(interfaceClass)) {
                return true;
            }
        }
        return false;
    }
}
