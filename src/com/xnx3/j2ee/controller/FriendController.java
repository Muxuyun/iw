package com.xnx3.j2ee.controller;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.xnx3.j2ee.entity.Friend;
import com.xnx3.j2ee.entity.Log;
import com.xnx3.j2ee.entity.User;
import com.xnx3.j2ee.service.FriendService;
import com.xnx3.j2ee.service.LogService;
import com.xnx3.j2ee.service.UserService;
import com.xnx3.j2ee.vo.FriendListVO;

/**
 * 好友
 * @author 管雷鸣
 *
 */
@Controller
@RequestMapping("/friend")
public class FriendController extends BaseController {
	
	@Resource
	private FriendService friendService;
	
	@Resource
	private UserService userService;
	
	@Resource
	private LogService logService;
	
	/**
	 * 好友功能的首页
	 * @param request
	 * @param response
	 * @return
	 */
	@RequiresPermissions("friendIndex")
	@RequestMapping("/index")
	public String index(HttpServletRequest request , HttpServletResponse response,Model model){
		return "friend/index";
	}

	/**
	 * 传入用户名/邮箱，根据此来添加好友
	 * @param param 用户名/邮箱 
	 * @param response
	 * @param request
	 * @throws IOException
	 */
	@RequiresPermissions("friendAdd")
	@RequestMapping("/add")
	public String add(String param ,HttpServletResponse response,HttpServletRequest request,Model model) throws IOException{
		List<User> findList=null;
		//判断是用户名还是邮箱
		if(param.indexOf("@")>0){
			findList=userService.findByEmail(param);
		}else{
			findList=userService.findByUsername(param);
		}
		
		if(findList.size()==0){
			return error(model, "要加的用户不存在！");
		}else{
			User otherUser=findList.get(0);	//要加的目标用户
			
			//继而查询是否曾添加过此人了，避免数据重复
			//List<Friend> friendList=friendDao.getSession().createQuery("SELECT * FROM friend WHERE self="+user.getId()+" AND other ="+otherUser.getId()).list();
			Friend selectFriend=new Friend();
			selectFriend.setId(getUser().getId());
			selectFriend.setOther(otherUser.getId());
			List<Friend> friendList=friendService.findByExample(selectFriend);
			
			if(friendList.size()>0){
				return error(model, "已经加过此人了，无需重复添加！");
			}else{
				Friend friend=new Friend();
				friend.setOther(otherUser.getId());
				friend.setSelf(getUser().getId());
				friendService.save(friend);
				
				Log log = new Log();
				log.setAddtime(new Date());
				log.setType(Log.typeMap.get("FRIEND_ADD"));
				log.setGoalid(otherUser.getId());
				log.setUserid(getUser().getId());
				logService.save(log);
				
				return success(model, "添加成功！");
			}
		}
	}
	
	/**
	 * 根据好友的userid删除好友
	 * @param id 好友的userid
	 * @param response
	 * @param request
	 * @throws IOException
	 */
	@RequiresPermissions("friendDelete")
	@RequestMapping("/delete")
	public String delete(int id ,HttpServletResponse response,HttpServletRequest request,Model model) throws IOException{
		Friend friendFind=new Friend();
		friendFind.setSelf(getUser().getId());
		friendFind.setOther(id);
		
		List<Friend> list= friendService.findByExample(friendFind);
		if(list.size()==0){
			return error(model, "要删除的好有不存在！");
		}else{
			Friend friend = list.get(0);
			friendService.delete(friend);
			
			Log log = new Log();
			log.setAddtime(new Date());
			log.setType(Log.typeMap.get("FRIEND_DELETE"));
			log.setGoalid(id);
			log.setUserid(getUser().getId());
			logService.save(log);
			
			return success(model, "删除成功");
		}
	}
	
	/**
	 * 返回当前的好友列表
	 * @param response
	 * @param request
	 * @throws IOException
	 */
	@RequiresPermissions("friendList")
	@RequestMapping("/list")
	@ResponseBody
	public FriendListVO list(){
		FriendListVO friendListVO = new FriendListVO();
		friendListVO.setList(friendService.findBySelf(getUser().getId()));
		return friendListVO;
	}
}
