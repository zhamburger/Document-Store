;================================Simple Boot Sector========================================

;loop:			; Define a label "loop" 
			; This will allow us to jump back to it forever 

;	jmp loop	; CPU instruction to jump 
			; This jumps to a specific memory address to continue eecution 
			; In this case its the current "loop" instruction



;============================Teletype Screen Interrupt=====================================


mov ah, 0x0e 		; Tells the video interrupt to write the contents of register 'al' in "tele-type mode"
			;
			; 0x10 is an ISR (interrupt service routine) relating to the screen 



;===============================Printing Registers=========================================

; Prints "Hello"
;
; mov al, 'H'
; int 0x10
; mov al, 'e'
; int 0x10
; mov al, 'l'
; int 0x10
; mov al, 'l'
; int 0x10
; mov al, 'o'
; int 0x10




;===============================Printing Pointers==========================================





;*******************************Global Offset***********************************

[org 0x7c00]		; Globally sets where the offsets should start
			; Attempt 2 will work : it now gets the correct offset 
			; Attempt 3 wont work : it now doubles the given offset 

;*******************************************************************************


; Getting a byte of data from an address to print as a character 


; First attempt 	; This DOESNT print "X"
mov al, "1"
int 0x10

mov al, the_secret	; Moves the offset of the_secret instead of what is at that offset 
int 0x10

; Second attempt 	; This DOESNT print "X"
mov al, "2"
int 0x10

mov al, [the_secret]	; "[]" tells the CPU to get the contents at that offset 
			; This still fails because it is getting the address offset from the start of memory 
			; Where we want the address offset from the start of our compiled code
int 0x10

; Third attempt		; This DOES print "X" 
mov al, "3"
int 0x10

mov bx, the_secret	; Move into 'bx'  the offset of the_secret 
add bx, 0x7c00		; Add to 'bx' the offset where we believe our compiled code to start 
mov al, [bx]		; Move the contents of 'bx' into 'al'
int 0x10		

; Fourth attempt 	; This DOES print "X"
mov al, "4"
int 0x10

mov al, [0x7c2d]	; Using pre-calculations because we know "X" lies in the 45th byte (2d)
			; Move (0x7c00 + 0x2d) = (0x7c2d) into 'al' 
int 0x10


jmp $			; Jump to current address forever 

the_secret:
	db "X"


; When running the code the output will be 1<random char> 2<random char> 3X4X
; Because attempt 3 and 4 are the only successful attempts



;*********************Global Offset************************
;
; The ouput will be 1<random char> 2X 3<random char> 4X
; Because now attempt 2 and 4 are the only successful ones
;
;**********************************************************






;=============================="Magic Number" Padding====================================



times 510-($-$$) db 0	; The program needs to fit into 512 bytes
			; The last two bytes need to be the "Magic Number"
			; This (db 0) instruction tells the compiler to pad out 510 '0's


dw 0xaa55		; "dw" is "Define Word" which will insert 2 bytes (size of a word) 
			;
			; Now we are at the 510th byte, we add the "Magic Number" 
			; The "Magic Number" "0x55" and "0xaa" tells the BIOS this is a boot sector



; Simple Boot Sector should look as such when running the following command 
; 
; "od -t x1 -A n <FILE.bin>"
;
; eb fe 00 00 00 00 00 00 00 00 00 00 00 00 00 00
; 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
; *
; 00 00 00 00 00 00 00 00 00 00 00 00 00 00 55 aa
; 
; Where "the first bytes" are the jump instrucion 
