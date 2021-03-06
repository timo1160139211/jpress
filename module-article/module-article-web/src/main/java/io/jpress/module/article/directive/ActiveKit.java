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
package io.jpress.module.article.directive;


import io.jboot.web.controller.JbootControllerContext;
import io.jpress.JPressActiveKit;
import io.jpress.module.article.model.Article;

import java.util.List;

public class ActiveKit {

    public static void setActiveFlagByCurrentArticle(List<Article> articles) {
        Article currentArticle = JbootControllerContext.get().getAttr("article");

        //当前页面并不是文章详情页面
        if (currentArticle == null) {
            return;
        }

        for (Article article : articles) {
            if (article.getId() != null && article.getId().equals(currentArticle.getId())) {
                JPressActiveKit.makeItActive(article);
            }
        }
    }

}
