/*******************************************************************************
 * Copyright 2012 The MITRE Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
/**
 * 
 */
package org.mitre.oauth2.web;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.mitre.oauth2.exception.ClientNotFoundException;
import org.mitre.oauth2.model.SystemScope;
import org.mitre.oauth2.service.ClientDetailsEntityService;
import org.mitre.oauth2.service.SystemScopeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.collect.Sets;

/**
 * @author jricher
 *
 */
@Controller
@SessionAttributes("authorizationRequest")
public class OAuthConfirmationController {

	@Autowired
	private ClientDetailsEntityService clientService;
	
	@Autowired
	private SystemScopeService scopeService;
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	public OAuthConfirmationController() {
		
	}
	
	public OAuthConfirmationController(ClientDetailsEntityService clientService) {
		this.clientService = clientService;
	}
	
	@PreAuthorize("hasRole('ROLE_USER')")
	@RequestMapping("/oauth/confirm_access")
	public ModelAndView confimAccess(Map<String, Object> model, @ModelAttribute("authorizationRequest") AuthorizationRequest clientAuth) {

		//AuthorizationRequest clientAuth = (AuthorizationRequest) model.remove("authorizationRequest");

		ClientDetails client = null;
		
		try {
			client = clientService.loadClientByClientId(clientAuth.getClientId());
		} catch (OAuth2Exception e) {	
			logger.error("OAuthConfirmationController: confirmAccess: OAuth2Exception was thrown when attempting to load client: "
					+ e.getStackTrace().toString());
			model.put("code", HttpStatus.BAD_REQUEST);
			return new ModelAndView("httpCodeView");
		} catch (IllegalArgumentException e) {
			logger.error("OAuthConfirmationController: confirmAccess: IllegalArgumentException was thrown when attempting to load client: "
					+ e.getStackTrace().toString());
			model.put("code", HttpStatus.BAD_REQUEST);
			return new ModelAndView("httpCodeView");
		}
		
		if (client == null) {
			logger.error("OAuthConfirmationController: confirmAccess: could not find client " + clientAuth.getClientId());
			model.put("code", HttpStatus.NOT_FOUND);
			return new ModelAndView("httpCodeView");		}

		model.put("auth_request", clientAuth);
		model.put("client", client);

		String redirect_uri = clientAuth.getAuthorizationParameters().get("redirect_uri");
		
        model.put("redirect_uri", redirect_uri);

        
        /*
        Map<String, Boolean> scopes = new HashMap<String, Boolean>();
        for (String scope : clientAuth.getScope()) {
	        scopes.put(scope, Boolean.TRUE);
        }
         */
        
        Set<SystemScope> scopes = scopeService.fromStrings(clientAuth.getScope());
        
        Set<SystemScope> sortedScopes = new LinkedHashSet<SystemScope>(scopes.size());
        Set<SystemScope> systemScopes = scopeService.getAll();
        
        // sort scopes for display
        for (SystemScope s : systemScopes) {
	        if (scopes.contains(s)) {
	        	sortedScopes.add(s);
	        }
        }
        
        sortedScopes.addAll(Sets.difference(scopes, systemScopes));
        
        model.put("scopes", sortedScopes);
        
        return new ModelAndView("oauth/approve", model);
	}

	/**
     * @return the clientService
     */
    public ClientDetailsEntityService getClientService() {
    	return clientService;
    }

	/**
     * @param clientService the clientService to set
     */
    public void setClientService(ClientDetailsEntityService clientService) {
    	this.clientService = clientService;
    }
	
	
}
