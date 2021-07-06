;=================================PRINT======================================

print:			; print method 
	pusha		; reset and push all the registers (new stack frame)

start: 
	mov al, [bx] 	; 'bs' address for the String
	cmp al, 0	; while(String[i] != 0)
	je done

	mov ah, 0x0e	; Use the BIOS to print
	int 0x10	; 'al' has the char 

	add bx, 1 	; increment pointer by one to the next char
	jmp start	; continue the loop 


done: 
	popa		; pop all the registers we used in this stack frame
	ret 		; return to the address that we saved in the 'call'

;===============================NEW LINE===================================

print_new_line:
	pusha		; reset the registers for this stack frame

	mov ah, 0x0e	; Use the BIOS to print 
	mov al, 0x0a	; print a 'newline' char '\n'
	int 0x10	
	mov al, 0x0d	; print 'carriage return' char '\r' 
	int 0x10

	popa		; pop all the registers that this stack frame used
	ret 		; return to the address that we saved in the 'call'
