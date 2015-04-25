package com.group7.hms;

import java.lang.ProcessBuilder.Redirect;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.group7.hms.Users.Administrator;
import com.group7.hms.Users.Patient;
import com.group7.hms.Users.Providers;
import com.group7.hms.Users.User;
import com.group7.hms.appointment.Appointment;
import com.group7.hms.dao.AppointmentDAO;
import com.group7.hms.dao.UserDAO;
import com.group7.hms.dao.UserDAOImpl;
import com.group7.hms.service.GeneratePDF;
import com.group7.hms.service.SendEmail;

/**
 * 
 * @author Richa Gandhi
 *
 */
@Controller
public class UserProfileController {
	private static final Logger logger = LoggerFactory
			.getLogger(UserProfileController.class);
	UserDAO daoObject = new UserDAOImpl();
	AppointmentDAO appDaoObject = new AppointmentDAO();
	@RequestMapping(value = "/signup", method = RequestMethod.GET)
	public String newUser(Locale locale, Model model) {
		logger.info("Accessing the new User Signup page.");

		model.addAttribute("viewName", "signup");

		return "masterpage";
	}

	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public String Login(Locale locale, Model model) {
		logger.info("Accessing the new User Login page.");

		model.addAttribute("viewName", "login");

		return "masterpage";
	}

	@RequestMapping(value = "/profile", method = RequestMethod.POST)
	public String LoginSubmit(Locale locale, Model model,
			@RequestParam(value = "email", defaultValue = "") String email,
			@RequestParam(value = "password", defaultValue = "") String password) {
		logger.info("Logging in" +email);
		
		UserDAOImpl dao = new UserDAOImpl();
		User userInfo = dao.getUserName(email);
		System.out.println(userInfo);
		if (userInfo.getName()==null){
			model.addAttribute("ErrorMsg", "Account doesn't exist, Please Signup");

			model.addAttribute("viewName", "signup");

			
		}
		else if (null==userInfo.getStatus()|| ("InActive").equalsIgnoreCase(userInfo.getStatus())) {

			return "redirect:/updateProfile";

		}
		else if (("active").equalsIgnoreCase(userInfo.getStatus())) {
			
			List<Appointment> appointmentList = appDaoObject.getAppointments(userInfo.getPrimaryEmail(), userInfo.getJobTitle());
			List<Appointment> billedList = null;
			if ((userInfo.getJobTitle()).equalsIgnoreCase("patient")){
				billedList = appDaoObject.getBilledAppointments(userInfo.getPrimaryEmail());
				GeneratePDF.generateBill();
				System.out.println("generated Bill");
			}
			model.addAttribute("user", userInfo);
			model.addAttribute("appList",appointmentList);
			model.addAttribute("billedList", billedList);
		//model.addAttribute("role", info[1]);
			System.out.println(email);
			//model.addAttribute("email",email);
			model.addAttribute("viewName", "Profile");
		}
		return "masterpage";
	}

	@RequestMapping(value = "/updateProfile", method = RequestMethod.GET)
	public String updateProfile(Locale locale, Model model,
			@RequestParam(value = "email", defaultValue = "") String email,
			@RequestParam(value = "password", defaultValue = "") String password) {
		
		User user = daoObject.getUser(email);
		if (user.getJobTitle().equalsIgnoreCase("Patient")){
			model.addAttribute("patient",user);
			model.addAttribute("viewName", "updatePatientProfile");
			//model.addAttribute("viewName","home");
			System.out.println(user);
		}
		else if( user.getJobTitle().equalsIgnoreCase("Doctor")||user.getJobTitle().equalsIgnoreCase("Nurse")){
			model.addAttribute("provider", user);
			model.addAttribute("viewName","updateProviderProfile");
			System.out.println(user);
		}
		else if (user.getJobTitle().equalsIgnoreCase("Admin")){
			model.addAttribute("admin",user);
			model.addAttribute("viewName","updateAdminProfile");
			System.out.println(user);
			
		}
		else {
			model.addAttribute("user", user);
			model.addAttribute("viewName", "updateProfile");
			System.out.println(user);
		}
		return "masterpage";
	}

	@RequestMapping(value = "/submit", method = RequestMethod.POST)
	public String createUser(
			Locale locale,
			Model model,
			@RequestParam(value = "name", defaultValue = "") String name,
			@RequestParam(value = "email", defaultValue = "") String email,
			@RequestParam(value = "password", defaultValue = "") String password,
			@RequestParam(value = "role", defaultValue = "") String role

	) {

		logger.info("Attempting to create new user.");
		User user = null;
		if (role.equalsIgnoreCase("Doctor") || role.equalsIgnoreCase("Nurse")) {
			user = new Providers(email, password, role, name);

		} else if (role.equalsIgnoreCase("Patient")) {
			user = new Patient(email, password, role, name);

		} else if (role.equalsIgnoreCase("Admin")) {
			user = new Administrator(email, password, role, name);

		}
		try {
			daoObject.setUser(user);

			logger.info("Here Printing " + role + " " + email + " " + password
					+ " " + name);
			logger.info("new user object created." + role + " " + email + " "
					+ password);
			// model.addAttribute("user", user);
			// model.addAttribute("email", email);;
			logger.info("Mail Object Created");
			boolean answer = SendEmail.generateAndSendEmail(email, name);
			logger.info("" + answer);
			model.addAttribute("name", name);
			model.addAttribute("viewName", "signupEmailConfirmation");
		} catch (SQLException e) {
			model.addAttribute("ErrorMsg",
					"Account already Exists, Please Login");
			model.addAttribute("viewName", "signup");
		}

		return "masterpage";
	}

	@RequestMapping(value = "/makeAppointment", method = RequestMethod.GET)
	public String makeAppointment(
			Locale locale,
			Model model,
			@RequestParam(value = "department", defaultValue = "General") String department) {
		System.out.println("Im here 1");
		List<Providers> docList = daoObject.getDoctorInfo(department);
		System.out.println("Im here 2");
		model.addAttribute("viewName", "makeAppointment");
		model.addAttribute("doctorList", docList);
		
		return "masterpage";
	}

	/*
	 * @RequestMapping(value = "/makeAppointmentAjax", method =
	 * RequestMethod.POST) public @ResponseBody Providers
	 * makeAppointmentAjax(Locale locale, Model model,
	 * 
	 * @RequestParam(value = "department", defaultValue = "General") String
	 * department){ List<Providers> docList =
	 * daoObject.getDoctorInfo(department);
	 * System.out.println("Im here n ajax"); //model.addAttribute("viewName",
	 * "makeAppointment"); //model.addAttribute("doctorList", docList); //return
	 * "masterpage"; Providers x=null; for(Providers a : docList){ x = a; }
	 * return x; }
	 */

	@RequestMapping(value = "/makeAppointment", method = RequestMethod.POST, params = "search")
	public String searchDocs(
			Locale locale,
			Model model,
			@RequestParam(value = "department", defaultValue = "") String department) {
		List<Providers> docList = daoObject.getDoctorInfo(department);
		model.addAttribute("viewName", "makeAppointment");
		model.addAttribute("doctorList", docList);
		// return "redirect:/makeAppointment";
		return "masterpage";
	}

	@RequestMapping(value = "/viewPatientProfile", method = RequestMethod.POST)
	public String viewPatient(Model model,@RequestParam(value = "email", defaultValue = "") String email,
			@RequestParam(value = "patientEmail", defaultValue = "") String patientEmail,
			@RequestParam(value = "releaseBill", defaultValue = "") String releaseBill,
			@RequestParam(value = "appId", defaultValue = "") String appId){
		System.out.println("Printing" + appId +"realaseBill" +releaseBill + "EMail" +patientEmail);
		if(patientEmail.isEmpty()){
			appDaoObject.releaseBill(releaseBill);
			RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
			ra.addFlashAttribute("email", email);
			return "redirect:/profile";
		}
	
		else {
			
			model.addAttribute("viewName", "viewPatientProfile");
			User patientInfo = daoObject.getUserName(patientEmail);
			System.out.println("Patient Inf" +patientInfo);
			User userInfo = daoObject.getUserName(email);
			model.addAttribute("patient", patientInfo);
			model.addAttribute("user", userInfo);
			model.addAttribute("appId", appId);
			return "masterpage";
		}
	}
	
	
	@RequestMapping(value = "/viewBill", method = RequestMethod.GET)
	public String viewBill(Model model,@RequestParam(value = "bill") List<Appointment> bill,
			@RequestParam(value = "email", defaultValue = "") String email){
		//System.out.println(bill);
		User userInfo = daoObject.getUserName(email);
		//GeneratePDF.generateBill(userInfo, bill);
		model.addAttribute("user", userInfo);
		model.addAttribute("viewName", "Profile");
		return "masterpage";
	}

}
