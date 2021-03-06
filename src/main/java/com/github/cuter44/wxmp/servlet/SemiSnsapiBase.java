package com.github.cuter44.wxmp.servlet;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.Properties;

//import org.apache.http.client.*;
import com.alibaba.fastjson.*;
import com.github.cuter44.nyafx.text.*;

import com.github.cuter44.wxmp.*;
import com.github.cuter44.wxmp.reqs.*;
import com.github.cuter44.wxmp.resps.*;
import com.github.cuter44.wxmp.util.*;

/** 网页授权(snsapi_base)的基础实现, 为网页前端取得当前用户的 code, 交由第三方换取 openid.
 *
 * 关于该 servlet 的工作流程请参见 <a href="http://mp.weixin.qq.com/wiki/17/c0f37d5704f0b64713d5d2c37b468d75.html">网页授权获取用户基本信息↗</a>
 *
 * 需要在微信客户端上执行
 *
 * <b>直至微信6.0为止都无法使用 Ajax 调用此方法</b>, 该尿性是由微信客户端造成的.
 *
 * <pre style="font-size:12px">

    GET /semi-snsapi-base.api
    取得 code

    <strong>参数</strong>
    redir   :url    , 可选, 允许带参数, 在 QueryString 中附加 code=:code 重定向. <b>请勿用作用户身份验证.</b>

    <strong>响应</strong>
    <i>当未附带 <code>redir</code> 参数时:</i>
    application/json
    code  :string , code.

    <i>当附带 <code>redir</code> 参数时:</i>
    302 Found
    Location: :${redir}?code=${code}

 * </pre>
 */
public class SemiSnsapiBase extends HttpServlet
{
    public static final String KEY_APPID     = "appid";
    public static final String KEY_SECRET    = "SECRET";

    protected static final String CODE      = "code";
    protected static final String REDIR     = "redir";

    protected String appid;
    protected String secret;

    /** 提供 appid 参数.
     * servlet 从此方法取得必需参数 appid, 覆盖此方法可以自定义 appid 的来源.
     * 默认实现从配置文件 /wxpay.properties 读取
     */
    public String getAppid(HttpServletRequest req)
        throws Exception
    {
        return(this.appid);
    }

    /** 处理 code.
     * 覆盖此方法可以自行构造响应
     * 默认实现如文档所述
     */
    public void code(String code, HttpServletRequest req, HttpServletResponse resp)
        throws Exception
    {
        String redir = req.getParameter(REDIR);
        if (redir == null)
        {
            resp.setContentType("application/json; charset=utf-8");
            resp.getWriter().print("{\"code\":\""+code+"\"}");

            return;
        }

        // else
        String rebuild = URLParser.fromURL(redir)
            .setParameter(CODE, code)
            .toURL();
        resp.sendRedirect(rebuild);

        return;
    }

    /** Trigger on error encountered.
     * <br />
     * This method can be overrided to plant your own error handling implemention.
     * <br />
     * Default behavior is <code>ex.printStackTrace()</code>
     */
    public void onError(Exception ex, HttpServletRequest req, HttpServletResponse resp)
        throws IOException, ServletException
    {
        this.log("ERROR: SemiSnsapiBase failed.", ex);
    }

    /** 读取配置文件
     * 覆盖此方法可以删除对配置文件的访问, 此情况下 getAppid(), getSecret() 均需要自行实现.
     */
    @Override
    public void init()
    {
        Properties conf = WxmpFactory.getDefaultInstance().getConf();
        this.appid = conf.getProperty(KEY_APPID);
        this.secret = conf.getProperty(KEY_SECRET);

        return;
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws IOException, ServletException
    {
        try
        {
            req.setCharacterEncoding("utf-8");

            String code = req.getParameter(CODE);

            if (code == null)
            {
                String thisUrl = URLBuilder.encodeURIComponent(
                    req.getRequestURL()
                        .append(req.getQueryString()!=null?"?"+req.getQueryString():"")
                        .toString()
                );

                String url = new Oauth2Authorize.SnsapiBase(this.getAppid(req), thisUrl)
                    .build()
                    .toURL();

                resp.sendRedirect(url);

                return;
            }

            // else
            this.code(code, req, resp);
        }
        catch (Exception ex)
        {
            this.onError(ex, req, resp);
        }

        return;
    }

}
