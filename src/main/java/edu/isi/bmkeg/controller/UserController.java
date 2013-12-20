package edu.isi.bmkeg.controller;

import java.util.ArrayList;
import java.util.List;

import edu.isi.bmkeg.vpdmf.model.VpdmfUser;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * This controller class is meant to be super-simple to understand for beginners.
 * We could have optimized it using wildcards in order to have less methods, but that would have made it harder to read.
 * @author misvy
 */

@Controller
public class UserController {
	
	/**
	 * Plain JSP
	 */
	@RequestMapping(value="/users/all/jsp-plain",method=RequestMethod.GET)
	public String findUsersPlain(Model model){
		buildUserList(model);
		model.addAttribute("title", "Users List - Plain JSP");
		return "01-plain/users";
	}
	
	/**
	 * JSP with custom tags
	 */
	@RequestMapping(value="/users/all/jsp-custom-1",method=RequestMethod.GET)
	public String findUsersTags(Model model){
		buildUserList(model);
		model.addAttribute("title", "Users List - Custom tags");
		return "02-custom-tags/users";
	}
	
	@RequestMapping(value="/users/all/jsp-custom-2",method=RequestMethod.GET)
	public String findUsersTableTag(Model model){
		buildUserList(model);
		model.addAttribute("title", "Users List - Custom tags");
		return "02-custom-tags/usersWithTableTag";
	}
	
	/**
	 * JSP with ThymeLeaf
	 */
	@RequestMapping(value="/users/all/thymeleaf",method=RequestMethod.GET)
	public String findUsersThymeLeaf(Model model){
		buildUserList(model);
		model.addAttribute("title", "Users List - Thymeleaf");
		return "thymeleaf/users";
	}

	private void buildUserList(Model model) {
		List<VpdmfUser> users = new ArrayList<VpdmfUser>();
		/*users.add(new User("Paul", "Chapman"));
		users.add(new User("Mike", "Wiesner"));
		users.add(new User("Mark", "Secrist"));
		users.add(new User("Ken", "Krueger"));
		users.add(new User("Wes", "Gruver"));
		users.add(new User("Kevin", "Crocker"));
		model.addAttribute("users", users);*/
	}
	
	
}
