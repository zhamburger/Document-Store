[org 0x7c00]			; set offset to boot sector code

;=============================MAIN===============================

; 'call' saves the address of execution
; so that the function can return to the control flow in main


mov bx, HELLO_WORLD
call print			; print(HELLO_WORLD)

call print_new_line		; print('\n')

mov bx, GOODBYE_WORLD
call print			; print(GOODBYE_WORLD)

call print_new_line

mov dx, 0x1fb6
call print_hex			; print_hex(0x1fb6)

jmp $


;=============================Imports============================

%include "print.asm"		; import print file
%include "print_hex.asm"	; import print hex file

;=============================Strings============================
HELLO_WORLD:
	db 'Hello, World', 0	

GOODBYE_WORLD:
	db 'Goodbye, World', 0

;============================Magic Number========================
times 510-($-$$) db 0
dw 0xaa55
