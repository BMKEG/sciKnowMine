package edu.isi.bmkeg.vpdmf.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.isi.bmkeg.vpdmf.dao.VpdmfDao;
import edu.isi.bmkeg.vpdmf.model.VpdmfUser;
import edu.isi.bmkeg.vpdmf.model.VpdmfUserRole;
import edu.isi.bmkeg.vpdmf.model.instances.LightViewInstance;
import edu.isi.bmkeg.vpdmf.model.qo.VpdmfUser_qo;

@Transactional
@Service("vpdmfUserService")
public class VpdmfUserDetailsServiceImpl implements UserDetailsService {

	@Autowired
	private VpdmfDao vpdmfDao;

	public VpdmfUserDetailsServiceImpl() {
	}

	public VpdmfUserDetailsServiceImpl(VpdmfDao dao) {
		this.vpdmfDao = dao;
	}

	public void setVpdmfDao(VpdmfDao dao) {
		this.vpdmfDao = dao;
	}

	public VpdmfDao getVpdmfDao() {
		return this.vpdmfDao;
	}

	@Override
	public UserDetails loadUserByUsername(String username)
			throws UsernameNotFoundException {

		VpdmfUser_qo qo = new VpdmfUser_qo();
		qo.setLogin(username);

		List<LightViewInstance> l = null;
		try {
			l = vpdmfDao.listVpdmfUser(qo);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		if (l.size() == 1) {
			LightViewInstance lvi = l.get(0);
			VpdmfUser domainUser = null;
			try {
				domainUser = vpdmfDao.findVpdmfUserById(lvi.getVpdmfId());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			boolean enabled = true;
			boolean accountNonExpired = true;
			boolean credentialsNonExpired = true;
			boolean accountNonLocked = true;

			Collection<GrantedAuthority> auths = new ArrayList<GrantedAuthority>();
			for (VpdmfUserRole r : domainUser.getRoles()) {
				auths.add(new SimpleGrantedAuthority(r.getRole()));
			}

			return new User(domainUser.getLogin(), domainUser.getPassword(),
					enabled, accountNonExpired, credentialsNonExpired,
					accountNonLocked, auths);

		} else {
			throw new UsernameNotFoundException(username + " not found");
		}

	}

}
