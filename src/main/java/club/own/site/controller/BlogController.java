package club.own.site.controller;

import club.own.site.bean.BlogItem;
import club.own.site.bean.Comment;
import club.own.site.bean.Member;
import club.own.site.config.redis.RedisClient;
import club.own.site.utils.EncodeUtils;
import club.own.site.utils.FileUploadUtils;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.management.StringValueExp;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static club.own.site.BlogCategoryEnum.getNameByCode;
import static club.own.site.constant.ProjectConstant.*;
import static club.own.site.utils.DateTimeUtils.getCurrentFormatTime;
import static club.own.site.utils.FileUploadUtils.getBasePath;

@Slf4j
@Controller
public class BlogController extends BaseController {

    @Autowired
    private RedisClient redisClient;

    @GetMapping(value = "blog/list")
    public @ResponseBody String blogList (){
        List<String> blogList = redisClient.sort(BLOG_LIST_KEY, "", 0, 3, false);
        List<BlogItem> blogItems = Lists.newArrayList();
        blogList.forEach(id -> {
            if (StringUtils.isNotBlank(id)) {
                String blogContent = redisClient.hget(BLOG_ITEM_KEY + id, BLOG_BODY_KEY);
                BlogItem blogItem = JSON.parseObject(blogContent, BlogItem.class);
                EncodeUtils.encodeObj(blogItem);
                blogItems.add(blogItem);
            }
        });

        return JSON.toJSONString(blogItems);
    }

    @GetMapping(value = "blog/comments")
    public @ResponseBody String blogComments (){
        List<String> blogList = redisClient.sort(BLOG_LIST_KEY, "", 0, 3, false);
        List<Comment> comments = Lists.newArrayList();
        blogList.forEach(s -> {
            if (StringUtils.isNotBlank(s)) {
                BlogItem blogItem = JSON.parseObject(s, BlogItem.class);
                EncodeUtils.encodeObj(blogItem);
                if (CollectionUtils.isNotEmpty(blogItem.getComments())) {
                    comments.addAll(blogItem.getComments());
                }
            }
        });

        return JSON.toJSONString(comments);
    }

    @GetMapping(value = "blog/del")
    public @ResponseBody String blogDel (@RequestParam(value = "idx", required = false) String idx){
        if (StringUtils.isNotBlank(idx)) {
            String blogJson = redisClient.lindex(BLOG_LIST_KEY, Integer.valueOf(idx));
            redisClient.lrem(BLOG_LIST_KEY, -1, blogJson);
        } else {
            String blogJson = redisClient.lpop(BLOG_LIST_KEY);
            if (StringUtils.isNotBlank(blogJson)) {
                BlogItem blogItem = JSON.parseObject(blogJson, BlogItem.class);
                EncodeUtils.encodeObj(blogItem);
                return "BLOG [" + blogItem.getTitle() + "], id=[" + blogItem.getId() +"] is deleted";
            }
        }
        return "OK";
    }

    @PostMapping(value = "blog/add")
    public @ResponseBody String add (MultipartHttpServletRequest request){
        Map<String, String> params = parseRequestParam(request.getParameterMap());
        List<MultipartFile> files = request.getFiles("photoArr");
        BlogItem blogItem = new BlogItem();
        blogItem.setTitle(params.get("title"));
        blogItem.setBrief(params.get("brief"));
        blogItem.setContent(params.get("content"));
        String mobile = params.get("mobile");
        String category = params.get("category");
        if (StringUtils.isNotBlank(mobile)) {
            String memberJson = redisClient.hget(MEMBER_LIST_KEY, mobile);
            Member member = JSON.parseObject(memberJson, Member.class);
            blogItem.setAuthor(member);
        }
        blogItem.setCreateTime(getCurrentFormatTime());
        String relativePath = "static/img/blog/" + blogItem.getId() + "/";
        if (CollectionUtils.isNotEmpty(files)) {
            for (int i=0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                if (file == null || file.isEmpty()) {
                    continue;
                }
                String desFileName = i + ".jpg";
                String fileUrl = relativePath + desFileName;
                if (!blogItem.getImgList().contains(fileUrl)) {
                    blogItem.getImgList().add(i, fileUrl);
                }
                if (StringUtils.isBlank(blogItem.getFirstImgUrl())){
                    blogItem.setFirstImgUrl(fileUrl);
                }
                String dirPath = getBasePath() + relativePath;
                FileUploadUtils.uploadFile(file, dirPath, desFileName);
            }
        }

        redisClient.rpush(BLOG_LIST_KEY, String.valueOf(blogItem.getId()));
        redisClient.rpush(BLOG_CATE_KEY + getNameByCode(category), String.valueOf(blogItem.getId()));
        redisClient.hset(BLOG_ITEM_KEY + blogItem.getId(), BLOG_BODY_KEY, JSON.toJSONString(blogItem));

        return "OK";
    }

}
