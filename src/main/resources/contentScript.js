/*
 * EML to PDF Converter
 * Copyright (C) 2015 Nick Russler
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

(function() {
	f = document.createElement('iframe');
	f.src = document.getElementById('header-v6a8oxpf48xfzy0rhjra').getAttribute('data-file');
	f.border = 0;
	f.id = 'header-frame-6jk3h6a234g2a3';
	var fs = f.style;
	fs.position = 'absolute';
	fs.overflow = 'hidden';
	fs.width = '1000px';
	fs.top = 0;
	fs.left = 0;
	fs.zIndex = 100003;
	fs.border = 'none';
	fs.padding = 0;
	fs.margin = 0;
	
	document.body.insertBefore(f, document.body.firstChild);

	var oldVal = parseInt(document.getElementsByTagName('html')[0].style.paddingTop) || 0;
	
	window.addEventListener('message', function(event) {
		document.getElementById('header-frame-6jk3h6a234g2a3').height = event.data;
		document.getElementsByTagName('html')[0].style.paddingTop = (oldVal + event.data) + 'px';
		
		// TODO push down all position absolute elements and remeber their old positions
//		var all = document.querySelector('*');
//		for(var i = 0; i < all.length; i++) {
//		  if (all[i].style.position == 'absolute' && all[i] !== f) {
//		    all[i].style.top = ((parseInt(all[i].style.top) || 0) + event.data) + 'px';
//		  }
//		}
	});
})();