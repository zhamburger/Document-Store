; Numeric ASCII Values: '0' (0x30) - '9' (0x39) 
; Alphabetical ACII Values: 'A' (0x41) - 'F' (0x46)


print_hex:
	pusha			; reset registers 

	mov cx, 4		; index


;============================Loop Through Hex===========================

loop: 				
	dec cx			; decrement 'cx'
	
	mov ax, dx		; 'ax' as working register
	shr dx, 4		; right shift 4 bits 
	and ax, 0xf		; mask the first 3 values to '0'

	mov bx, HEX_OUTPUT	; set bx to the address of the output 
	add bx, 2		; skip over the '0x'
	add bx, cx		; add the counter to the address

	cmp ax, 0xa		; if(current_char < 0x41) //is a number
	jl add_letter
	add al, 0x27		; Its a letter
				; 0x27 + 0x30 (add_letter) = 0x61 'a'
				; Which is the value starting 'a'-'f'
	jl add_letter

;========================Add Letter To Output============================

add_letter:
	add al, 0x30		; ASCII hex starts at 0x30
	mov byte [bx], al	; Add the value to the byte at 'bx'

	cmp cx, 0		; if(index == 0)
	je done_loop		; 	return
	jmp loop		; else
				; 	continue 


;===========================Return String================================	

done_loop:
	mov bx, HEX_OUTPUT
	call print
	
	popa
	ret

;===========================Output String================================
HEX_OUTPUT:
	db '0x0000', 0 
