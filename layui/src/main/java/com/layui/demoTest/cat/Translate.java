package com.layui.demoTest.cat;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.MessageProducer;
import com.dianping.cat.message.Transaction;

/**
 * 这是网上的例子：没什么卵用
 * 这是我们一个对外提供的翻译服务，getWordMean为服务控制器入口，
 * 我们将原始消息展开如下，整个服务处理耗时571ms，一进来我们会记录URL类型事务，
 * 以及调用参数，然后记录用户校验函数，在函数内部，有查询用户信息的数据库操作，
 * 也会被记录下，查询耗时150ms，接下来我们会先从缓存获取结果， 缓存查询耗时 59ms，
 * 随后我们翻译内容，翻译之后我们会调用语音服务提供的发音接口，voice/getVoice，
 * 发音接口调用一共耗时 361 ms。
 */
@Controller
public class Translate {
	public Map<String, String> maps = new HashMap<String, String>();

	public Cat.Context context;

	@RequestMapping(value = "/translate/getWordMean", produces = "application/json")
	@ResponseBody
	// 获取翻译释义
	public Object getWordMean(HttpServletRequest request, HttpServletResponse response) {
		context = new Cat.Context() {
			@Override
			public void addProperty(String key, String value) {
				maps.put(key, value);
			}

			@Override
			public String getProperty(String key) {
				return maps.get(key);
			}
		}; // 服务调用消息上下文

		MessageProducer cat = Cat.getProducer();
		Transaction t = cat.newTransaction("URL", "translate/getWordMean"); // 你的接口/方法名称

		cat.logEvent("ClientInfo", "RemoteIp=127.0.0.1&Referer=..."); // 记录远程调用端信息
		cat.logEvent("Payload", "HTTP/GET /translate/getWordMean?client=3&clientVersion=0&v=9.5&uid=3214567...."); // 调用端参数

		// 用户校验
		authCheck();

		// 先从缓存Redis获取结果
		Transaction cacheT = cat.newTransaction("Cache.memcached.redis", "translate_result:get");
		cat.logEvent("Cache.memcached.redis.server", "127.0.0.1:6379");

		// do your cache operation

		cacheT.setStatus(Message.SUCCESS);
		cacheT.complete();

		// do your translate operation

		// 记录远程语音服务调用
		Transaction callT = cat.newTransaction("Call", "voice:getVoice");
		Cat.logEvent("Call.server", "localhost"); // 远程服务地址
		Cat.logEvent("Call.app", "voice"); // 语音服务
		Cat.logEvent("Call.port", "8080"); // 语音服务端口
		Cat.logRemoteCallClient(context); // 生成消息调用上下文，主要是几个messageId的创建。

		voiceService(context);

		callT.setStatus(Message.SUCCESS);
		callT.complete();
		//
		// OutputData result = new OutputData();
		// result.setErrno(0);
		// result.setErrmsg("success");
		// result.setTranslateResult("translate result ...");

		t.setStatus(Message.SUCCESS);
		t.complete();

		return null;
	}

	public boolean authCheck() {
		MessageProducer cat = Cat.getProducer();
		Transaction checkUser = cat.newTransaction("Method", "checkAuth");

		// 从数据库查询用户信息
		Transaction sqlT = cat.newTransaction("SQL", "Select");

		cat.logEvent("SQL.Database", "jdbc:mysql://127.0.0.1:3306/user");
		cat.logEvent("SQL.Method", "select");
		cat.logEvent("SQL.Statement", "SELECT", Message.SUCCESS, "select * from user_info");

		// to do your SQL query

		sqlT.setStatus(Message.SUCCESS);
		sqlT.complete();

		// to do your auth check

		checkUser.setStatus(Message.SUCCESS);
		checkUser.complete();
		return true;
	}

	// 线程模拟语音服务
	protected void voiceService(final Cat.Context context) {
		Thread thread = new Thread() {
			@Override
			public void run() {
				// 服务器埋点，Domain为 voice 提供语音服务
				Cat.getManager().getThreadLocalMessageTree().setDomain("voice");
				MessageProducer cat = Cat.getProducer();
				Transaction voiceService = Cat.newTransaction("URL", "voice/getVoice"); // 你的接口/方法名称
				cat.logEvent("ClientInfo", "RemoteIp=127.0.0.1&Referer=..."); // 记录远程调用端信息
				cat.logEvent("Payload", "HTTP/GET /voice/getVoice?client=3&clientVersion=0&v=9.5&uid=3214567...."); // 调用端参数

				// 记录服务信息
				Transaction child = Cat.newTransaction("Service", "voice:getVoice");
				Cat.logEvent("Service.client", "localhost"); // 客户端地址
				Cat.logEvent("Service.app", "translate"); // 客户端domain
				Cat.logRemoteCallServer(context); // 记录消息上下文

				// to do your business

				child.setStatus(Message.SUCCESS);
				child.complete();

				voiceService.setStatus(Message.SUCCESS);
				voiceService.complete();

			}
		};

		thread.start();

		// wait for it to complete
		try {
			thread.join();
		} catch (InterruptedException e) {
			// ignore it
		}
	}
}
