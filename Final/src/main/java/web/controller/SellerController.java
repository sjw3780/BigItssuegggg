package web.controller;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import web.dto.BookListInfo;
import web.dto.Chat;
import web.dto.Message;
import web.dto.SellerInfo;
import web.dto.SellerLoc;
import web.dto.User;
import web.service.face.ChatService;
import web.service.face.SellerService;

@Controller
public class SellerController {
	
	private static final Logger logger
	= LoggerFactory.getLogger(SellerController.class);
	
	@Autowired SellerService sellerService;
	@Autowired ChatService chatService;
	
	@RequestMapping(value="/seller/main", method=RequestMethod.GET)
	public String sellerMain(
			@RequestParam(defaultValue="-1") int chatRoomNo, HttpSession session, Model model) { 
		logger.info("chatRoomNo:"+chatRoomNo);

		session.setAttribute("chatRoomNo", chatRoomNo);
		
		//-------------------채팅내역 방들(사이드에 보이는 채팅방들)을 보여지기 위한 model설정--------------------
		User LoginInfo = (User)session.getAttribute("LoginInfo");
		logger.info("LoginInfo:"+LoginInfo);
		if(LoginInfo==null) {
			return "seller/main";
		}
			
		List<Chat> chatRoomList = chatService.selectRooms(LoginInfo);
		
		//채팅내역의 상대방 이름을 띄워 주기 위해서 추가
		//Chat의 TheOtherParty에
		//chatRoomList속에 있는 Chat를 하나하나 조사해서 로그인된 아이디와 같지않고 null이 아닌 아이디를 넣어주자.
		for(int i=0; i<chatRoomList.size(); i++) {
			if(chatRoomList.get(i).getBuyerId() != null && !chatRoomList.get(i).getBuyerId().equals(LoginInfo.getId())) {
				chatRoomList.get(i).setTheOtherParty(chatRoomList.get(i).getBuyerId());
			}else if(chatRoomList.get(i).getSellerId() != null && !chatRoomList.get(i).getSellerId().equals(LoginInfo.getId())) {
				chatRoomList.get(i).setTheOtherParty(chatRoomList.get(i).getBuyerId());
			}else if(chatRoomList.get(i).getBigdomId() != null && !chatRoomList.get(i).getBigdomId().equals(LoginInfo.getId())) {
				chatRoomList.get(i).setTheOtherParty(chatRoomList.get(i).getBuyerId());
			}
		}
		
		logger.info("chatRoomList:"+chatRoomList);
		
		model.addAttribute("chatRoomList", chatRoomList);
		//--------------------------------------------------------------------------------
		
		//--------------------채팅메시지 -----------------------------------------------------
		List<Message> primaryMsgList = new ArrayList();
		List<Message> subMsgList = new ArrayList();
		//DB에 저장되어 있는 Message불러오기.	
		
		//메인 채팅방(가운데에 있는 채팅방)에서의 메시지
		primaryMsgList = chatService.selectMessage(chatRoomNo);
		
		//메인 채팅 리스트 Date이쁘게 바꾸기
		//Date보기 좋게 변경
		SimpleDateFormat date = new SimpleDateFormat("yyyy/MM/dd");
		SimpleDateFormat time = new SimpleDateFormat("hh:mm a");
		
		Date tempTime = null;
		String stringTime = null;

		for(int i=0; i<primaryMsgList.size(); i++) {
			//현재 date받아오기
			tempTime = primaryMsgList.get(i).getChatDate();		
			//date를 이쁜String으로 바꾸기
			stringTime = time.format(tempTime);		
			//primaryMsgList에 stringChatDate설정
			primaryMsgList.get(i).setStringChatDate(stringTime);
		}
			
		//채팅내역 방들(사이드에 보이는 채팅방들)에서의 메시지
		for(int i=0; i<chatRoomList.size(); i++) {
			//보조 메시지(가장 마지막의 메시지 하나만 가져와야함)
			Message subMsg = chatService.selectSubMessage(chatRoomList.get(i).getChatRoomNo());
			//현재 date받아오기
			tempTime = subMsg.getChatDate();
			//date를 이쁜String으로 바꾸기
			stringTime = time.format(tempTime);		
			//subMsgList에 stringChatDate설정
			subMsg.setStringChatDate(stringTime);

			if(subMsg != null) {
				subMsgList.add(subMsg);
			}
			
		}
		
		//주채팅창 message list
		logger.info("primaryMsgList:"+primaryMsgList);
		model.addAttribute("primaryMsgList", primaryMsgList);
		
		//보조채팅창 message list
		logger.info("subMsgList:"+subMsgList);
		model.addAttribute("subMsgList", subMsgList);
		
		return "seller/main";
	}
	
	@RequestMapping(value="/seller/login", method=RequestMethod.POST)
	public String sellerLogin(
			SellerInfo sellerInfo,
			HttpSession session) {
		
		//chat에서 session정보를 가져올때
		//User라는(판매자,빅돔,구매자)정보 모두를 포함하는 dto의 정보를  불러와야해서 만듦.
		User LoginInfo = null;
		
		// 로그인
		if(sellerService.login(sellerInfo)) { // 로그인 성공 시
			session.setAttribute("sellerLogin", true);
			session.setAttribute("sellerId", sellerInfo.getSellerId());
//			logger.info(sellerInfo.toString());
			
			// 판매자 전체 정보 가져오기
			sellerInfo = sellerService.getSellerInfo(sellerInfo.getSellerId());
//			logger.info(sellerInfo.toString());
			
			//User으로 정보가 필요해서 추가
			LoginInfo = sellerService.getSellerInfoUser(sellerInfo);
			session.setAttribute("LoginInfo", LoginInfo);
		}
		
		
		return "redirect:/seller/main";
	}
	
	@RequestMapping(value="/seller/logout", method=RequestMethod.GET)
	public String sellerLogout(HttpSession session) {
		
		session.invalidate();
		
		return "redirect:/seller/main";
	}
	
	@RequestMapping(value="/seller/time", method=RequestMethod.GET)
	public void sellerTime(
			SellerLoc sellerLoc, Model model,
			HttpSession session) {
		
		String sellerId = (String) session.getAttribute("sellerId");
		
		// 판매시간 조회
		sellerLoc = sellerService.getSellerLoc(sellerId);
		
//		logger.info(sellerLoc.toString());
		
		String startTime = sellerLoc.getSellerTimeS();
		String startTime1 = startTime.substring(0, 2);
		String startTime2 = startTime.substring(2, 4);
		
		String endTime = sellerLoc.getSellerTimeE();
		String endTime1 = endTime.substring(0, 2);
		String endTime2 = endTime.substring(2, 4);
		
//		logger.info(startTime1 + ":" + startTime2);
		//---------------

		// 판매부수 조회
		List<BookListInfo> bookListInfo = sellerService.getBookList(sellerId);
		
		logger.info(bookListInfo.toString());
		
		model.addAttribute("startTime1", startTime1);
		model.addAttribute("startTime2", startTime2);
		model.addAttribute("endTime1", endTime1);
		model.addAttribute("endTime2", endTime2);
		
		model.addAttribute("bookListInfo", bookListInfo);
		
	}
	
	@RequestMapping(value="/seller/updateTime", method=RequestMethod.POST)
	public String updateTime(SellerLoc sellerLoc, HttpSession session) {
		
		logger.info("변경할 시간:"+sellerLoc);
		
		// 세션값 가져오기
		String sellerId = (String) session.getAttribute("sellerId");
		sellerLoc.setSellerId(sellerId);
		
		// 변경할 시간 세팅
		String sellerTimeS = sellerLoc.getStartTime1()+sellerLoc.getStartTime2();
		String sellerTimeE = sellerLoc.getEndTime1()+sellerLoc.getEndTime2();
		sellerLoc.setSellerTimeS(sellerTimeS);
		sellerLoc.setSellerTimeE(sellerTimeE);
		
		// 시간 변경
		sellerService.setSellerTime(sellerLoc);
		
		return "redirect:/seller/time";
	}
	
	@RequestMapping(value="/seller/mUpdate", method=RequestMethod.POST)
	public String mUpdate(BookListInfo bookListInfo) {
		
//		logger.info("수정할 내용:"+bookListInfo.toString());
		
		// 판매 호수, 판매 부수 수정
		sellerService.setMegazine(bookListInfo);
		
		return "redirect:/seller/time";
	}
	
	@RequestMapping(value="/seller/mDelete", method=RequestMethod.GET)
	public String mDelete(BookListInfo bookListInfo) {
		
//		logger.info("삭제할 내용:"+bookListInfo.toString());
		
		// 판매 호수, 판매 부수 삭제
		sellerService.deleteMegazine(bookListInfo.getMagazineNo());
		
		return "redirect:/seller/time";
	}
	
	@RequestMapping(value="/seller/mPlus", method=RequestMethod.POST)
	public String mPlus(BookListInfo bookListInfo, HttpSession session) {
		
		// 세션값 가져오기
		String sellerId = (String) session.getAttribute("sellerId");
		bookListInfo.setSellerId(sellerId);
		
//		logger.info("추가할 내용:"+bookListInfo.toString()+sellerId);
		
		// 판매할 빅이슈 추가하기
		sellerService.putMegazine(bookListInfo);
		
		return "redirect:/seller/time";
	}

}
