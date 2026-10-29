/**
 * Jappy - Japs Transfort Mascot JS Component
 * Provides window.Jappy.svg(mood) matching the approved prototype drawing exactly.
 */
(function (global) {
    'use strict';

    function renderJappySvg(m, size, extraClass) {
        var mood = m || 'happy';
        var glow = {
            happy: '#1f8a5b',
            worried: '#d64545',
            surprised: '#e69a1c',
            hello: '#f2b632',
            thinking: '#4a8fd6'
        }[mood] || '#f2b632';

        var S = '#c78a14', D = '#12392b';
        var eyeY = 80, eyes = '';

        if (mood === 'happy') {
            eyes = '<path d="M41 84 Q52 70 63 84" stroke="' + D + '" stroke-width="4.5" stroke-linecap="round" fill="none"/>' +
                   '<path d="M77 84 Q88 70 99 84" stroke="' + D + '" stroke-width="4.5" stroke-linecap="round" fill="none"/>';
        } else {
            var r = mood === 'surprised' ? 12 : 10;
            var p = mood === 'surprised' ? 4.5 : 6.2;
            var ox = mood === 'thinking' ? 3 : 0;
            var oy = mood === 'thinking' ? -3 : (mood === 'worried' ? 2 : 0);

            eyes = '<g class="blink">' +
                   '<circle cx="52" cy="80" r="' + r + '" fill="#fff" stroke="' + S + '" stroke-width="1.5"/>' +
                   '<circle cx="88" cy="80" r="' + r + '" fill="#fff" stroke="' + S + '" stroke-width="1.5"/>' +
                   '<circle cx="' + (52 + ox) + '" cy="' + (80 + oy) + '" r="' + p + '" fill="' + D + '"/>' +
                   '<circle cx="' + (88 + ox) + '" cy="' + (80 + oy) + '" r="' + p + '" fill="' + D + '"/>' +
                   '<circle cx="' + (54 + ox) + '" cy="' + (77.5 + oy) + '" r="2" fill="#fff"/>' +
                   '<circle cx="' + (90 + ox) + '" cy="' + (77.5 + oy) + '" r="2" fill="#fff"/>' +
                   '</g>';
        }

        var brow = mood === 'worried'
            ? '<path d="M40 64 L60 69 M100 64 L80 69" stroke="' + D + '" stroke-width="4" stroke-linecap="round"/>'
            : (mood === 'surprised'
                ? '<path d="M42 62 Q52 56 62 62 M78 62 Q88 56 98 62" stroke="' + D + '" stroke-width="3.5" stroke-linecap="round" fill="none"/>'
                : (mood === 'thinking'
                    ? '<path d="M42 66 Q52 62 62 66 M80 63 L98 59" stroke="' + D + '" stroke-width="3.5" stroke-linecap="round" fill="none"/>'
                    : ''));

        var cheeks = (mood === 'happy' || mood === 'hello')
            ? '<ellipse cx="38" cy="97" rx="7" ry="4.5" fill="#f28b82" opacity=".55"/><ellipse cx="102" cy="97" rx="7" ry="4.5" fill="#f28b82" opacity=".55"/>'
            : '';

        var mouth = {
            happy: '<path d="M55 95 Q70 114 85 95 Z" fill="#7a2a2a" stroke="' + D + '" stroke-width="3" stroke-linejoin="round"/><path d="M62 104 Q70 99 78 104 Q70 111 62 104Z" fill="#f28b82"/>',
            hello: '<path d="M55 95 Q70 114 85 95 Z" fill="#7a2a2a" stroke="' + D + '" stroke-width="3" stroke-linejoin="round"/><path d="M62 104 Q70 99 78 104 Q70 111 62 104Z" fill="#f28b82"/>',
            worried: '<path d="M57 107 Q70 95 83 107" stroke="' + D + '" stroke-width="4.5" stroke-linecap="round" fill="none"/>',
            surprised: '<ellipse cx="70" cy="103" rx="6.5" ry="8.5" fill="#7a2a2a" stroke="' + D + '" stroke-width="3"/>',
            thinking: '<path d="M60 104 Q70 100 80 105" stroke="' + D + '" stroke-width="4.5" stroke-linecap="round" fill="none"/>'
        }[mood] || '';

        var extra = {
            worried: '<path d="M118 56 q7 11 0 16 q-7 -5 0 -16z" fill="#7cc8ff" stroke="#3b9ae0" stroke-width="1.5"/>',
            happy: '<path d="M120 42 l3.2 7.6 7.6 3.2 -7.6 3.2 -3.2 7.6 -3.2 -7.6 -7.6 -3.2 7.6 -3.2z" fill="#fff" stroke="#f2b632" stroke-width="1.5"/><path d="M18 54 l2.2 5 5 2.2 -5 2.2 -2.2 5 -2.2 -5 -5 -2.2 5 -2.2z" fill="#fff" stroke="#f2b632" stroke-width="1.2"/>',
            surprised: '<path d="M118 50 l8 -8 M122 62 h10 M20 50 l-8 -8 M18 62 h-10" stroke="#e69a1c" stroke-width="3.5" stroke-linecap="round"/>',
            thinking: '<circle cx="116" cy="50" r="3" fill="' + glow + '"/><circle cx="126" cy="40" r="4.5" fill="' + glow + '"/><circle cx="118" cy="22" r="9" fill="#fff" stroke="' + glow + '" stroke-width="2"/>',
            hello: ''
        }[mood] || '';

        var hand = '<circle cx="21" cy="96" r="10" fill="url(#jb)" stroke="' + S + '" stroke-width="2.5"/>';
        var rh = mood === 'hello'
            ? '<g class="wave"><path d="M112 92 Q126 84 130 66" stroke="' + S + '" stroke-width="9" stroke-linecap="round" fill="none"/><path d="M112 92 Q126 84 130 66" stroke="#f2b632" stroke-width="5" stroke-linecap="round" fill="none"/><circle cx="131" cy="62" r="10" fill="url(#jb)" stroke="' + S + '" stroke-width="2.5"/></g>'
            : '<circle cx="119" cy="96" r="10" fill="url(#jb)" stroke="' + S + '" stroke-width="2.5"/>';

        var classes = 'j bob' + (extraClass ? ' ' + extraClass : '');
        var styleAttr = size ? ' style="width:' + size + ';height:' + size + ';"' : '';

        return '<svg class="' + classes + '"' + styleAttr + ' viewBox="0 0 140 140" aria-hidden="true">' +
            '<circle cx="70" cy="74" r="62" fill="' + glow + '" opacity=".15"/>' +
            '<ellipse cx="70" cy="131" rx="34" ry="5" fill="#000" opacity=".18"/>' +
            '<rect x="46" y="118" width="17" height="11" rx="5.5" fill="#d99a1f" stroke="' + S + '" stroke-width="2"/><rect x="77" y="118" width="17" height="11" rx="5.5" fill="#d99a1f" stroke="' + S + '" stroke-width="2"/>' +
            hand +
            '<rect x="26" y="40" width="88" height="82" rx="22" fill="url(#jb)" stroke="' + S + '" stroke-width="2.5"/>' +
            '<path d="M26 62 Q26 40 48 40 H92 Q114 40 114 62 Z" fill="url(#jl)"/>' +
            '<rect x="62" y="40" width="16" height="22" fill="#fff" opacity=".6"/><path d="M66 46 H74 M66 52 H74" stroke="#f2b632" stroke-width="1.5" opacity=".7"/>' +
            '<ellipse cx="27" cy="96" rx="2.5" ry="7" fill="' + S + '" opacity=".4"/><ellipse cx="113" cy="96" rx="2.5" ry="7" fill="' + S + '" opacity=".4"/>' +
            '<path d="M35 76 Q35 66 43 62" stroke="#fff" stroke-width="4.5" stroke-linecap="round" opacity=".45" fill="none"/>' +
            eyes + brow + cheeks + mouth + rh +
            '<ellipse cx="70" cy="39" rx="29" ry="6.5" fill="#0c2a1f"/><path d="M43 38 Q43 12 70 12 Q97 12 97 38 Z" fill="url(#jc)" stroke="#0c2a1f" stroke-width="2"/>' +
            '<circle cx="70" cy="27" r="8.5" fill="#f2b632" stroke="#d99a1f" stroke-width="2"/><text x="70" y="31" text-anchor="middle" font-size="12" font-weight="800" fill="#12392b" font-family="system-ui,sans-serif">J</text>' +
            extra +
            '</svg>';
    }

    // Expose public API
    global.Jappy = {
        svg: renderJappySvg
    };

    // Alias for compatibility
    if (!global.jappy) {
        global.jappy = renderJappySvg;
    }
})(typeof window !== 'undefined' ? window : this);
